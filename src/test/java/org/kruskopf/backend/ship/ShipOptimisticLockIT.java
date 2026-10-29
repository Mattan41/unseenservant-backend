package org.kruskopf.backend.ship;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.kruskopf.backend.AbstractIntegrationTest;
import org.kruskopf.backend.campaign.entity.Campaign;
import org.kruskopf.backend.campaign.entity.CampaignRole;
import org.kruskopf.backend.campaign.entity.CampaignUser;
import org.kruskopf.backend.campaign.repository.CampaignRepository;
import org.kruskopf.backend.campaign.repository.CampaignUserRepository;
import org.kruskopf.backend.exception.ConcurrentUpdateException;
import org.kruskopf.backend.ship.dto.ShipDTO;
import org.kruskopf.backend.ship.dto.ShipInputDTO;
import org.kruskopf.backend.ship.entity.Ship;
import org.kruskopf.backend.ship.repository.ShipRepository;
import org.kruskopf.backend.ship.service.ShipService;
import org.kruskopf.backend.testsupport.TestDataFactory;
import org.kruskopf.backend.user.entity.User;
import org.kruskopf.backend.user.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Proves that ship edits are protected against lost updates at the database
 * level, not only by the version token the client sends.
 * <p>
 * Deliberately <strong>not</strong> {@code @Transactional}: the protection lives
 * in the {@code UPDATE ... WHERE version = ?} that Hibernate issues on flush, and
 * a test-managed transaction that never commits can never observe a conflict.
 * <p>
 * The MySQL container is reused between runs
 * ({@code TestContainersConfiguration} sets {@code withReuse}), so every fixture
 * gets a unique name and is removed again in {@link #tearDown()}; nothing here
 * assumes an empty database.
 */
@DisplayName("Ship optimistic locking")
class ShipOptimisticLockIT extends AbstractIntegrationTest {

    private final ShipService shipService;
    private final ShipRepository shipRepository;
    private final CampaignRepository campaignRepository;
    private final CampaignUserRepository campaignUserRepository;
    private final UserRepository userRepository;
    private final JdbcTemplate jdbcTemplate;
    private final TransactionTemplate transactionTemplate;

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    ShipOptimisticLockIT(ShipService shipService,
                         ShipRepository shipRepository,
                         CampaignRepository campaignRepository,
                         CampaignUserRepository campaignUserRepository,
                         UserRepository userRepository,
                         JdbcTemplate jdbcTemplate,
                         TransactionTemplate transactionTemplate) {
        this.shipService = shipService;
        this.shipRepository = shipRepository;
        this.campaignRepository = campaignRepository;
        this.campaignUserRepository = campaignUserRepository;
        this.userRepository = userRepository;
        this.jdbcTemplate = jdbcTemplate;
        this.transactionTemplate = transactionTemplate;
    }

    private User user;
    private Campaign campaign;

    @BeforeEach
    void setUp() {
        user = userRepository.save(TestDataFactory.aUser("shiplock-" + UUID.randomUUID()));
        campaign = campaignRepository.save(TestDataFactory.aCampaign(user));
        campaignUserRepository.save(new CampaignUser(campaign, user, CampaignRole.GM, "GM"));
    }

    @AfterEach
    void tearDown() {
        if (campaign != null && campaign.getId() != null) {
            shipRepository.findByCampaignId(campaign.getId()).ifPresent(shipRepository::delete);
            campaignUserRepository.deleteAll(campaignUserRepository.findByCampaignId(campaign.getId()));
            campaignRepository.deleteById(campaign.getId());
        }
        if (user != null && user.getId() != null) {
            userRepository.deleteById(user.getId());
        }
    }

    @Test
    @DisplayName("a normal update increments the version in the database")
    void updateIncrementsVersion() {
        // Covers the sharp edge of the primitive version field: a ship that has
        // never been modified sits at 0, and Spring Data still has to treat it as
        // an existing row (it decides by id, not by the version).
        ShipDTO created = shipService.saveShip(campaign.getId(), shipInput("The Desert Rose", null, null), user.getId());
        assertThat(created.version()).isZero();

        ShipDTO updated = shipService.saveShip(campaign.getId(), shipInput(null, 7, created.version()), user.getId());

        assertThat(updated.hull()).isEqualTo(7);
        assertThat(updated.version()).isEqualTo(1L);
        assertThat(shipRepository.findById(created.id()).orElseThrow().getVersion())
                .as("the new version must be persisted, not just returned")
                .isEqualTo(1L);
    }

    @Test
    @DisplayName("a stale client version is rejected before any work is done")
    void staleClientVersionIsRejected() {
        ShipDTO created = shipService.saveShip(campaign.getId(), shipInput("The Desert Rose", null, null), user.getId());
        shipService.saveShip(campaign.getId(), shipInput(null, 7, created.version()), user.getId());

        // Everything but the call under test is arranged first: a throwing setup
        // call inside the lambda would be caught by assertThatThrownBy and read as
        // the rejection this test is about.
        Long campaignId = campaign.getId();
        long userId = user.getId();
        ShipInputDTO staleInput = shipInput(null, 3, created.version());

        assertThatThrownBy(() -> shipService.saveShip(campaignId, staleInput, userId))
                .isInstanceOf(ConcurrentUpdateException.class);
    }

    @Test
    @DisplayName("a flush against a row that changed underneath is rejected by the version column")
    void staleFlushIsRejectedByTheVersionColumn() {
        ShipDTO created = shipService.saveShip(campaign.getId(), shipInput("The Desert Rose", null, null), user.getId());
        Long shipId = created.id();

        assertThatThrownBy(() -> transactionTemplate.executeWithoutResult(status -> {
            Ship ship = shipRepository.findById(shipId).orElseThrow();
            ship.setHull(9); // dirty, so Hibernate will actually issue the UPDATE

            // A competing writer bumps the row first. This is the case the
            // service-level check cannot see: it runs before this flush.
            jdbcTemplate.update("UPDATE ship SET version = version + 1 WHERE id = ?", shipId);

            // UPDATE ship SET ... , version = 1 WHERE id = ? AND version = 0 -> 0 rows.
            shipRepository.saveAndFlush(ship);
        }))
                .as("Hibernate must reject the stale flush with the exception the "
                        + "controller advice maps to 409")
                .isInstanceOf(ObjectOptimisticLockingFailureException.class);

        Ship after = shipRepository.findById(shipId).orElseThrow();
        assertThat(after.getHull())
                .as("the stale write must not have landed")
                .isEqualTo(Ship.DEFAULT_HULL);
    }

    private static ShipInputDTO shipInput(String name, Integer hull, Long version) {
        return new ShipInputDTO(name, hull, null, null, null, null, null, null, null, null, null, version);
    }
}
