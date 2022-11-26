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
@Table(name = "field")
public class Field {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(nullable = false)
    private long id;

    public Field(String name, String selector, float weight) {
        this.name = name;
        this.selector = selector;
        this.weight = weight;
    }

    @Column( nullable = false)
    private String name;

    @Column(nullable = false)
    private String selector;

    @Column(nullable = false)
    private float weight;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Field field = (Field) o;
        return id == field.id && Float.compare(field.weight, weight) == 0 && Objects.equals(name, field.name) && Objects.equals(selector, field.selector);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, selector, weight);
    }

}
