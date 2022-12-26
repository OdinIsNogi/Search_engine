package searchengine.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import java.util.Objects;
import java.util.Set;


@Setter
@Getter
@NoArgsConstructor
@Entity
@Table(name = "pages")
public class Page {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @ManyToOne(cascade = {CascadeType.DETACH, CascadeType.MERGE, CascadeType.REFRESH,CascadeType.REMOVE})
    @JoinColumn(name = "site_id", referencedColumnName = "id")
    private Site site;

    @Column(nullable = false)
    private String path;

    @Column(nullable = false)
    private int code;

    @Column(columnDefinition = "MEDIUMTEXT", nullable = false)
    private String context;

    @OneToMany(mappedBy = "page", cascade = CascadeType.ALL)
    private Set<Index> indexes;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Page page = (Page) o;
        return code == page.code && Objects.equals(site, page.site) && Objects.equals(path, page.path) && Objects.equals(context, page.context);
    }

    @Override
    public int hashCode() {
        return Objects.hash(site, path, code, context);
    }

    @Override
    public String toString() {
        return "Page{" +
                "id=" + id +
                ", site=" + site +
                ", path='" + path + '\'' +
                ", code=" + code +
                '}';
    }
}
