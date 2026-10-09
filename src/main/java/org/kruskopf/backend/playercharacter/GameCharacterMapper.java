package org.kruskopf.backend.playercharacter;

import org.kruskopf.backend.campaign.entity.Campaign;
import org.kruskopf.backend.dnd5e.dto.Dnd5eCharacterDataInputDTO;
import org.kruskopf.backend.dnd5e.dto.Dnd5eCharacterDataOutputDTO;
import org.kruskopf.backend.dnd5e.entity.Dnd5eCharacterData;
import org.kruskopf.backend.offworlders.dto.OffworldersCharacterDataInputDTO;
import org.kruskopf.backend.offworlders.dto.OffworldersCharacterDataOutputDTO;
import org.kruskopf.backend.offworlders.entity.OffworldersCharacterData;
import org.kruskopf.backend.playercharacter.dto.CharacterInputDTO;
import org.kruskopf.backend.playercharacter.dto.CharacterOutputDTO;
import org.kruskopf.backend.playercharacter.entity.GameCharacter;
import org.kruskopf.backend.playercharacter.entity.GameSystem;
import org.kruskopf.backend.user.entity.User;
import org.springframework.stereotype.Component;

import java.util.ArrayList;

@Component
public class GameCharacterMapper {

    public CharacterOutputDTO toOutputDTO(GameCharacter character) {
        return new CharacterOutputDTO(
                character.getId(),
                character.getOwner().getId(),
                character.getCampaign() != null ? character.getCampaign().getId() : null,
                character.getName(),
                character.getSystemType(),
                character.getNotes(),
                character.getImageUrl(),
                toDnd5eOutputDTO(character),
                toOffworldersOutputDTO(character),
                character.getCreatedAt(),
                character.getUpdatedAt()
        );
    }

    private Dnd5eCharacterDataOutputDTO toDnd5eOutputDTO(GameCharacter character) {
        if (character.getSystemType() != GameSystem.DND5E) {
            return null;
        }
        Dnd5eCharacterData data = character.getDnd5eData();
        if (data == null) {
            return null;
        }
        return new Dnd5eCharacterDataOutputDTO(
                data.getLevel(),
                data.getCharacterClass(),
                data.getRace(),
                data.getHitPoints(),
                data.getArmorClass(),
                data.getStats()
        );
    }

    private OffworldersCharacterDataOutputDTO toOffworldersOutputDTO(GameCharacter character) {
        if (character.getSystemType() != GameSystem.OFFWORLDERS) {
            return null;
        }
        OffworldersCharacterData data = character.getOffworldersData();
        if (data == null) {
            return null;
        }
        return new OffworldersCharacterDataOutputDTO(
                data.getCharacterClass(),
                data.getSpecies(),
                data.getLook(),
                data.getXp(),
                data.getHealth(),
                data.getArmor(),
                data.getSupply(),
                data.getSupplyMax(),
                data.getCredits(),
                data.getStats(),
                data.getSkills(),
                data.getAbilities(),
                data.getGear()
        );
    }

    public GameCharacter toEntity(CharacterInputDTO dto, User owner, Campaign campaign) {
        GameSystem system = dto.systemType() != null ? dto.systemType() : GameSystem.DND5E;

        GameCharacter character = new GameCharacter(owner, campaign, dto.name(), system);
        character.setNotes(dto.notes());
        if (dto.avatarUrl() != null) {
            character.setImageUrl(dto.avatarUrl());
        }

        if (system == GameSystem.DND5E) {
            character.attachDnd5eData(toDnd5eEntity(dto.dnd5e()));
        } else if (system == GameSystem.OFFWORLDERS) {
            character.attachOffworldersData(toOffworldersEntity(dto.offworlders()));
        }

        return character;
    }

    private Dnd5eCharacterData toDnd5eEntity(Dnd5eCharacterDataInputDTO dto) {
        Dnd5eCharacterData data = new Dnd5eCharacterData();
        if (dto != null) {
            if (dto.level() != null) data.setLevel(dto.level());
            if (dto.characterClass() != null) data.setCharacterClass(dto.characterClass());
            if (dto.race() != null) data.setRace(dto.race());
            if (dto.hitPoints() != null) data.setHitPoints(dto.hitPoints());
            if (dto.armorClass() != null) data.setArmorClass(dto.armorClass());
            if (dto.stats() != null) data.setStats(dto.stats());
        }
        return data;
    }

    private OffworldersCharacterData toOffworldersEntity(OffworldersCharacterDataInputDTO dto) {
        OffworldersCharacterData data = new OffworldersCharacterData();
        if (dto != null) {
            if (dto.characterClass() != null) data.setCharacterClass(dto.characterClass());
            if (dto.species() != null) data.setSpecies(dto.species());
            if (dto.look() != null) data.setLook(dto.look());
            if (dto.xp() != null) data.setXp(dto.xp());
            if (dto.health() != null) data.setHealth(dto.health());
            if (dto.armor() != null) data.setArmor(dto.armor());
            if (dto.supply() != null) data.setSupply(dto.supply());
            if (dto.supplyMax() != null) data.setSupplyMax(dto.supplyMax());
            if (dto.credits() != null) data.setCredits(dto.credits());
            if (dto.stats() != null) data.setStats(dto.stats());
            if (dto.skills() != null) data.setSkills(new ArrayList<>(dto.skills()));
            if (dto.abilities() != null) data.setAbilities(new ArrayList<>(dto.abilities()));
            if (dto.gear() != null) data.setGear(dto.gear());
        }
        return data;
    }

    public void patchEntity(GameCharacter entity, CharacterInputDTO dto) {
        if (dto.name() != null) entity.setName(dto.name());
        if (dto.systemType() != null) entity.setSystemType(dto.systemType());
        if (dto.notes() != null) entity.setNotes(dto.notes());
        // this is to be able to update the imageUrl
        if (dto.avatarUrl() != null) entity.setImageUrl(dto.avatarUrl());

        // Patch system-specific data only when the active system is D&D 5e.
        if (dto.dnd5e() != null && entity.getSystemType() == GameSystem.DND5E) {
            Dnd5eCharacterData data = entity.getDnd5eData();
            if (data == null) {
                entity.attachDnd5eData(toDnd5eEntity(dto.dnd5e()));
            } else {
                patchDnd5e(data, dto.dnd5e());
            }
        }

        // Patch system-specific data only when the active system is Offworlders.
        if (dto.offworlders() != null && entity.getSystemType() == GameSystem.OFFWORLDERS) {
            OffworldersCharacterData data = entity.getOffworldersData();
            if (data == null) {
                entity.attachOffworldersData(toOffworldersEntity(dto.offworlders()));
            } else {
                patchOffworlders(data, dto.offworlders());
            }
        }
    }

    private void patchDnd5e(Dnd5eCharacterData data, Dnd5eCharacterDataInputDTO dto) {
        if (dto.level() != null) data.setLevel(dto.level());
        if (dto.characterClass() != null) data.setCharacterClass(dto.characterClass());
        if (dto.race() != null) data.setRace(dto.race());
        if (dto.hitPoints() != null) data.setHitPoints(dto.hitPoints());
        if (dto.armorClass() != null) data.setArmorClass(dto.armorClass());
        if (dto.stats() != null) data.setStats(dto.stats());
    }

    private void patchOffworlders(OffworldersCharacterData data, OffworldersCharacterDataInputDTO dto) {
        if (dto.characterClass() != null) data.setCharacterClass(dto.characterClass());
        if (dto.species() != null) data.setSpecies(dto.species());
        if (dto.look() != null) data.setLook(dto.look());
        if (dto.xp() != null) data.setXp(dto.xp());
        if (dto.health() != null) data.setHealth(dto.health());
        if (dto.armor() != null) data.setArmor(dto.armor());
        if (dto.supply() != null) data.setSupply(dto.supply());
        if (dto.supplyMax() != null) data.setSupplyMax(dto.supplyMax());
        if (dto.credits() != null) data.setCredits(dto.credits());
        if (dto.stats() != null) data.setStats(dto.stats());
        if (dto.skills() != null) data.setSkills(new ArrayList<>(dto.skills()));
        if (dto.abilities() != null) data.setAbilities(new ArrayList<>(dto.abilities()));
        if (dto.gear() != null) data.setGear(dto.gear());
    }
}
