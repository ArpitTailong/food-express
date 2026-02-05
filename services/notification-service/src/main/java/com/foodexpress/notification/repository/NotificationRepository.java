package com.foodexpress.notification.repository;

import com.foodexpress.notification.domain.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, String> {
    
    // User notifications
    Page<Notification> findByUserIdOrderByCreatedAtDesc(String userId, Pageable pageable);
    
    @Query("SELECT n FROM Notification n WHERE n.userId = :userId AND n.channel = 'IN_APP' ORDER BY n.createdAt DESC")
    Page<Notification> findInAppByUserId(@Param("userId") String userId, Pageable pageable);
    
    @Query("SELECT n FROM Notification n WHERE n.userId = :userId AND n.isRead = false AND n.channel = 'IN_APP'")
    List<Notification> findUnreadByUserId(@Param("userId") String userId);
    
    long countByUserIdAndIsReadFalseAndChannel(String userId, NotificationChannel channel);
    
    // Retry queue
    @Query("""
            SELECT n FROM Notification n 
            WHERE n.status != 'DELIVERED' 
            AND n.status != 'CANCELLED' 
            AND n.retryCount < n.maxRetries 
            AND n.nextRetryAt <= :now
            ORDER BY n.nextRetryAt ASC
            """)
    List<Notification> findNotificationsToRetry(@Param("now") LocalDateTime now);
    
    // Pending notifications
    @Query("SELECT n FROM Notification n WHERE n.status = 'PENDING' ORDER BY n.createdAt ASC")
    List<Notification> findPendingNotifications(Pageable pageable);
    
    // Mark as read
    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true, n.readAt = :now WHERE n.userId = :userId AND n.isRead = false")
    int markAllAsRead(@Param("userId") String userId, @Param("now") LocalDateTime now);
    
    // Stats
    @Query("SELECT COUNT(n) FROM Notification n WHERE n.status = :status AND n.createdAt >= :since")
    long countByStatusSince(@Param("status") NotificationStatus status, @Param("since") LocalDateTime since);
    
    @Query("SELECT COUNT(n) FROM Notification n WHERE n.channel = :channel AND n.createdAt >= :since")
    long countByChannelSince(@Param("channel") NotificationChannel channel, @Param("since") LocalDateTime since);
    
    // Cleanup old notifications
    @Modifying
    @Query("DELETE FROM Notification n WHERE n.createdAt < :before AND n.status IN ('DELIVERED', 'FAILED', 'CANCELLED')")
    int deleteOldNotifications(@Param("before") LocalDateTime before);
}
