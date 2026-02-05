package com.foodexpress.user.repository;

import com.foodexpress.user.domain.UserProfile;
import com.foodexpress.user.domain.UserRole;
import com.foodexpress.user.domain.UserStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<UserProfile, String> {
    
    Optional<UserProfile> findByEmail(String email);
    
    Optional<UserProfile> findByPhoneNumber(String phoneNumber);
    
    boolean existsByEmail(String email);
    
    boolean existsByPhoneNumber(String phoneNumber);
    
    // Find active users by role
    @Query("SELECT u FROM UserProfile u WHERE u.role = :role AND u.status = 'ACTIVE'")
    Page<UserProfile> findActiveByRole(@Param("role") UserRole role, Pageable pageable);
    
    // Find available drivers (for assignment)
    @Query("SELECT u FROM UserProfile u WHERE u.role = 'DRIVER' AND u.status = 'ACTIVE' AND u.isAvailable = true")
    List<UserProfile> findAvailableDrivers();
    
    // Find available drivers sorted by rating
    @Query("SELECT u FROM UserProfile u WHERE u.role = 'DRIVER' AND u.status = 'ACTIVE' AND u.isAvailable = true ORDER BY u.averageRating DESC NULLS LAST")
    List<UserProfile> findAvailableDriversByRating();
    
    // Search users
    @Query("""
            SELECT u FROM UserProfile u 
            WHERE u.status != 'DELETED' 
            AND (LOWER(u.firstName) LIKE LOWER(CONCAT('%', :query, '%'))
                OR LOWER(u.lastName) LIKE LOWER(CONCAT('%', :query, '%'))
                OR LOWER(u.email) LIKE LOWER(CONCAT('%', :query, '%'))
                OR u.phoneNumber LIKE CONCAT('%', :query, '%'))
            """)
    Page<UserProfile> searchUsers(@Param("query") String query, Pageable pageable);
    
    // Find users excluding deleted
    @Query("SELECT u FROM UserProfile u WHERE u.status != 'DELETED'")
    Page<UserProfile> findAllActive(Pageable pageable);
    
    // Count by status
    long countByStatus(UserStatus status);
    
    // Count by role
    long countByRole(UserRole role);
}
