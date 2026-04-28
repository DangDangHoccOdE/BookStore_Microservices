package com.bookstore.user.domain;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserProfileRepository extends JpaRepository<UserProfileEntity, UUID> {}
