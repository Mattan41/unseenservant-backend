package org.kruskopf.backend.journal.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import org.kruskopf.backend.entity.BaseEntity;

/**
 * One numbered page of a {@link Journal}, holding markdown source.
 * <p>
 * {@link #position} is the page's place in the book (1-based) and is what the
 * client shows as the page number. It is deliberately <em>not</em> the page's
 * identity: {@link #id} is stable, while a page inserted after page {@code P}
 * shifts every later page by one and a deleted page shifts them back.
 * <p>
 * {@link #version} provides optimistic concurrency in the same two layers the
 * ship uses: the service compares the token the client sent against the freshly
 * loaded value ({@code ConcurrentUpdateException}, 409), and Hibernate's
 * {@code UPDATE ... WHERE version = ?} catches a writer that committed between
 * the load and the flush.
 */
@Entity
@Table(name = "journal_page", indexes = {
        @Index(name = "idx_journal_page_journal_position", columnList = "journal_id,position")
})
public class JournalPage extends BaseEntity {

    @ManyToOne
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "journal_id", nullable = false)
    private Journal journal;

    /** Markdown source. A blank page is a page with empty or whitespace content. */
    @Column(columnDefinition = "LONGTEXT")
    private String content = "";

    /** 1-based place in the book; the number the client displays. */
    @Column(name = "position", nullable = false)
    private int position;

    /**
     * Optimistic-concurrency version, owned by Hibernate. Never assign this: the
     * service layer compares the client's token against the loaded value, and
     * Hibernate performs the increment on flush.
     */
    @Version
    @Column(nullable = false)
    private long version = 0;

    public JournalPage() {
    }

    public JournalPage(Journal journal, int position, String content) {
        this.journal = journal;
        this.position = position;
        this.content = content == null ? "" : content;
    }

    public Journal getJournal() {
        return journal;
    }

    public void setJournal(Journal journal) {
        this.journal = journal;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content == null ? "" : content;
    }

    public int getPosition() {
        return position;
    }

    public void setPosition(int position) {
        this.position = position;
    }

    public long getVersion() {
        return version;
    }

    /** For tests only. Production code must let Hibernate manage the version. */
    public void setVersion(long version) {
        this.version = version;
    }

}
