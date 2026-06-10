package atmin.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "courts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Court extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "court_name", length = 50, nullable = false)
    private String courtName;

    @Column(name = "type", length = 50)
    private String type;

    @Column(name = "image_url", length = 255)
    private String imageUrl;

    @Column(name = "is_available")
    private boolean isAvailable;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cluster_id")
    private BadmintonCluster cluster;
}
