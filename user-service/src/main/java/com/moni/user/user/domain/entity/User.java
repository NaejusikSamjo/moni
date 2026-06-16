package com.moni.user.user.domain.entity;

import com.moni.common.JpaAuditing.baseEntity.BaseEntity;
import com.moni.user.user.domain.enums.OAuthProvider;
import com.moni.user.user.domain.enums.UserRole;
import com.moni.user.user.domain.enums.UserStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Persistable;

import java.util.UUID;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseEntity implements Persistable<UUID> {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Transient
    private boolean isNew;

    @Column(name = "email", length = 100, nullable = false, unique = true)
    private String email;

    @Column(name = "password")
    private String password;

    @Column(name = "name", length = 50, nullable = false)
    private String name;

    @Column(name = "nickname", length = 50, nullable = false)
    private String nickname;

    @Column(name = "phone", length = 20)
    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(name = "oauth_provider", length = 20)
    private OAuthProvider oauthProvider;

    @Column(name = "oauth_id")
    private String oauthId;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", length = 10, nullable = false)
    private UserRole role;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 10, nullable = false)
    private UserStatus status;

    @Column(name = "suspended_reason", columnDefinition = "TEXT")
    private String suspendedReason;

    @Column(name = "deleted_reason", columnDefinition = "TEXT")
    private String deletedReason;

    @Override
    public boolean isNew() {
        return isNew;
    }

    public static User create(String email, String password, String name, String nickname, String phone) {
        User user = new User();
        user.id = UUID.randomUUID();
        user.isNew = true;
        user.email = email;
        user.password = password;
        user.name = name;
        user.nickname = nickname;
        user.phone = phone;
        user.role = UserRole.USER;
        user.status = UserStatus.ACTIVE;
        return user;
    }

    public static User createOAuth(String email, String name, String nickname,
                                   OAuthProvider oauthProvider, String oauthId) {
        User user = new User();
        user.id = UUID.randomUUID();
        user.isNew = true;
        user.email = email;
        user.name = name;
        user.nickname = nickname;
        user.oauthProvider = oauthProvider;
        user.oauthId = oauthId;
        user.role = UserRole.USER;
        user.status = UserStatus.ACTIVE;
        return user;
    }

    public void updateProfile(String name, String nickname, String phone) {
        if (name != null) this.name = name;
        if (nickname != null) this.nickname = nickname;
        if (phone != null) this.phone = phone;
    }

    public void updatePassword(String encodedPassword) {
        this.password = encodedPassword;
    }

    public void suspend(String reason) {
        this.status = UserStatus.SUSPENDED;
        this.suspendedReason = reason;
    }

    public void withdraw(String reason, String deletedBy) {
        this.status = UserStatus.DELETED;
        this.deletedReason = reason;
        this.delete(deletedBy);
    }
}