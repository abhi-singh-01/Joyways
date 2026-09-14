package in.joyway.domain.repository;

import in.joyway.domain.entity.Coupon;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CouponRepository extends JpaRepository<Coupon, Long> {

    Optional<Coupon> findByCodeIgnoreCase(String code);

    boolean existsByCodeIgnoreCase(String code);

    @Query("""
            SELECT c FROM Coupon c
            JOIN FETCH c.travelPackage p
            WHERE (:query = '' OR lower(c.code) LIKE lower(concat('%', cast(:query as string), '%'))
               OR lower(p.title) LIKE lower(concat('%', cast(:query as string), '%')))
              AND (:active IS NULL OR c.active = :active)
              AND (:packageId IS NULL OR p.id = :packageId)
            """)
    Page<Coupon> search(
            @Param("query") String query,
            @Param("active") Boolean active,
            @Param("packageId") Long packageId,
            Pageable pageable
    );

    @Query("SELECT c FROM Coupon c JOIN FETCH c.travelPackage WHERE c.id = :id")
    Optional<Coupon> findByIdWithPackage(@Param("id") Long id);
}
