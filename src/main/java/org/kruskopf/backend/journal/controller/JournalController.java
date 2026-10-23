package org.kruskopf.backend.journal.controller;

import org.kruskopf.backend.journal.dto.JournalDTO;
import org.kruskopf.backend.journal.dto.JournalPageCreateDTO;
import org.kruskopf.backend.journal.dto.JournalPageSaveDTO;
import org.kruskopf.backend.journal.dto.JournalPageUpdateDTO;
import org.kruskopf.backend.journal.service.JournalService;
import org.kruskopf.backend.user.CustomUserDetails;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * A participant's private journal.
 * <p>
 * Every endpoint is scoped to the caller: the campaign id in the path and the
 * page id in the body, never another user's journal. Ownership and participation
 * rules live in {@link JournalService}; this is the coarse role gate.
 */
@RestController
@RequestMapping("/api/journals")
@PreAuthorize("hasRole('ROLE_USER')")
public class JournalController {

    private final JournalService journalService;

    public JournalController(JournalService journalService) {
        this.journalService = journalService;
    }

    @GetMapping("/campaign/{campaignId}")
    public ResponseEntity<JournalDTO> getJournal(@PathVariable Long campaignId,
                                                @AuthenticationPrincipal CustomUserDetails customUserDetails) {
        Long userId = customUserDetails.user().getId();
        return ResponseEntity.ok(journalService.getJournal(campaignId, userId));
    }

    @PostMapping("/campaign/{campaignId}/pages")
    public ResponseEntity<JournalDTO> createPage(@PathVariable Long campaignId,
                                                 @RequestBody(required = false) JournalPageCreateDTO dto,
                                                 @AuthenticationPrincipal CustomUserDetails customUserDetails) {
        Long userId = customUserDetails.user().getId();
        JournalPageCreateDTO effectiveDTO = dto != null ? dto : new JournalPageCreateDTO(null, null);

        JournalDTO journal = journalService.createPage(
                campaignId, userId, effectiveDTO.afterPageId(), effectiveDTO.content());
        return ResponseEntity.status(HttpStatus.CREATED).body(journal);
    }

    @PatchMapping("/pages/{pageId}")
    public ResponseEntity<JournalPageSaveDTO> updatePage(@PathVariable Long pageId,
                                                         @RequestBody JournalPageUpdateDTO dto,
                                                         @AuthenticationPrincipal CustomUserDetails customUserDetails) {
        Long userId = customUserDetails.user().getId();
        return ResponseEntity.ok(journalService.updatePage(pageId, userId, dto.content(), dto.version()));
    }

    @DeleteMapping("/pages/{pageId}")
    public ResponseEntity<JournalDTO> deletePage(@PathVariable Long pageId,
                                                 @AuthenticationPrincipal CustomUserDetails customUserDetails) {
        Long userId = customUserDetails.user().getId();
        return ResponseEntity.ok(journalService.deletePage(pageId, userId));
    }
}
