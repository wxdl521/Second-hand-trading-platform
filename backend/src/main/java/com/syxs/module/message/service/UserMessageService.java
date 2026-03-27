package com.syxs.module.message.service;

import com.syxs.common.exception.BusinessException;
import com.syxs.module.message.entity.UserMessage;
import com.syxs.module.message.repository.UserMessageRepository;
import com.syxs.module.user.entity.User;
import com.syxs.module.user.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserMessageService {

    private final UserMessageRepository userMessageRepository;
    private final UserRepository userRepository;

    public UserMessageService(UserMessageRepository userMessageRepository,
                              UserRepository userRepository) {
        this.userMessageRepository = userMessageRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<UserMessage> listMessages(String operatorPhone) {
        User operator = findUser(operatorPhone);
        return userMessageRepository.findAllByUserIdOrderByCreatedAtDescIdDesc(operator.getId());
    }

    @Transactional
    public UserMessage markRead(Long id, String operatorPhone) {
        User operator = findUser(operatorPhone);
        UserMessage message = userMessageRepository.findById(id)
            .orElseThrow(() -> new BusinessException("Message not found"));
        if (!operator.getId().equals(message.getUserId())) {
            throw new BusinessException("You are not allowed to access this message");
        }
        message.setIsRead(true);
        message.setReadAt(LocalDateTime.now());
        return userMessageRepository.save(message);
    }

    @Transactional
    public void createForUserId(Long userId, String type, String title, String content) {
        if (userId == null) {
            return;
        }
        UserMessage message = new UserMessage();
        message.setUserId(userId);
        message.setType(type);
        message.setTitle(title);
        message.setContent(content);
        message.setIsRead(false);
        message.setCreatedAt(LocalDateTime.now());
        userMessageRepository.save(message);
    }

    @Transactional
    public void createForUserPhone(String phone, String type, String title, String content) {
        if (phone == null || phone.isBlank()) {
            return;
        }
        userRepository.findByPhone(phone).ifPresent(user -> createForUserId(user.getId(), type, title, content));
    }

    private User findUser(String phone) {
        return userRepository.findByPhone(phone)
            .orElseThrow(() -> new BusinessException("User not found"));
    }
}
