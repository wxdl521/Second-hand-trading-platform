package com.syxs.module.message.repository;

import com.syxs.module.message.entity.UserMessage;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserMessageRepository extends JpaRepository<UserMessage, Long> {

    List<UserMessage> findAllByUserIdOrderByCreatedAtDescIdDesc(Long userId);
}
