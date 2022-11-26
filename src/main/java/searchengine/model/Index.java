package searchengine.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import javax.persistence.*;
import java.util.Objects;

@ToString
@Getter
@Setter
@Entity
@NoArgsConstructor
@Table(name = "`index`")
public class Index {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(nullable = false)
    private long id;

    @Column( name = "`rank`", nullable = false)
    private float rank;

    @ManyToOne(cascade = {CascadeType.PERSIST})
    @JoinColumn(name = "lemma_id",referencedColumnName = "id")
    private Lemma lemma;

    @ManyToOne(cascade = {CascadeType.PERSIST})
    @JoinColumn(name = "page_id",referencedColumnName = "id")
    private Page page;

    public Index(float rank, Lemma lemma, Page page) {
        this.rank = rank;
        this.lemma = lemma;
        this.page = page;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Index index = (Index) o;
        return id == index.id && Float.compare(index.rank, rank) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, rank);
    }


}
