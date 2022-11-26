package searchengine.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.stereotype.Component;

import javax.persistence.*;
import java.util.Date;
import java.util.Objects;
import java.util.Set;


@Component
@Getter
@Setter
@Table(name = "site")
@NoArgsConstructor
@Entity
public class Site {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(nullable = false)
    private long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", columnDefinition = "ENUM('M','S')", nullable = false)
    private Status status;

    @Column(name = "status_time", nullable = false)
    private Date statusTime;

    @Column(columnDefinition = "TEXT", name = "last_error")
    private String lastError;

    @Column(nullable = false)
    private String url;

    @Column(nullable = false)
    private String name;

    @Transient
    @OneToMany(mappedBy = "site", cascade = CascadeType.ALL)
    @JoinColumn(name = "page_id", referencedColumnName = "id")
    private Set<Page> pages;

    @Transient
    @OneToMany(mappedBy = "site", cascade = CascadeType.ALL)
    @JoinColumn(name = "lemma_id", referencedColumnName = "id")
    private Set<Lemma> lemmas;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Site site = (Site) o;
        return Objects.equals(url, site.url) && Objects.equals(name, site.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(url, name);
    }

    @Override
    public String toString() {
        return "Site{" +
                "id=" + id +
                ", status=" + status +
                ", statusTime=" + statusTime +
                ", lastError='" + lastError + '\'' +
                ", url='" + url + '\'' +
                ", name='" + name + '\'' +
                '}';
    }
}
