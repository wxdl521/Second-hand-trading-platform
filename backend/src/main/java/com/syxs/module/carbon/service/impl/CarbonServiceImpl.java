package com.syxs.module.carbon.service.impl;

import com.syxs.common.exception.BusinessException;
import com.syxs.config.FileStorageConfig;
import com.syxs.module.carbon.entity.CarbonRecord;
import com.syxs.module.carbon.repository.CarbonAccountRepository;
import com.syxs.module.carbon.repository.CarbonRecordRepository;
import com.syxs.module.carbon.service.CarbonService;
import com.syxs.module.user.entity.User;
import com.syxs.module.user.repository.UserRepository;
import java.awt.Color;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.imageio.ImageIO;
import org.springframework.stereotype.Service;

@Service
public class CarbonServiceImpl implements CarbonService {

    private static final Pattern KG_PATTERN = Pattern.compile("(\\d+(?:\\.\\d+)?)kg", Pattern.CASE_INSENSITIVE);

    private final CarbonAccountRepository carbonAccountRepository;
    private final CarbonRecordRepository carbonRecordRepository;
    private final UserRepository userRepository;
    private final FileStorageConfig fileStorageConfig;

    public CarbonServiceImpl(CarbonAccountRepository carbonAccountRepository,
                             CarbonRecordRepository carbonRecordRepository,
                             UserRepository userRepository,
                             FileStorageConfig fileStorageConfig) {
        this.carbonAccountRepository = carbonAccountRepository;
        this.carbonRecordRepository = carbonRecordRepository;
        this.userRepository = userRepository;
        this.fileStorageConfig = fileStorageConfig;
    }

    @Override
    public Map<String, Object> summary(String operatorPhone) {
        User user = findUser(operatorPhone);
        List<CarbonRecord> records = carbonRecordRepository.findAllByUserIdOrderByBizDateDescIdDesc(user.getId());
        String currentMonth = LocalDate.now().toString().substring(0, 7);
        Map<String, Object> summary = new LinkedHashMap<>();
        int balance = carbonAccountRepository.findByUserId(user.getId())
            .map(account -> account.getBalance() == null ? 0 : account.getBalance())
            .orElse(0);
        summary.put("balance", balance);
        summary.put("level", resolveLevel(balance));
        summary.put(
            "monthCarbonSavedKg",
            roundToTwo(
                records.stream()
                    .filter(record -> record != null
                        && isIncome(record.getType())
                        && record.getBizDate() != null
                        && record.getBizDate().startsWith(currentMonth))
                    .mapToDouble(this::extractCarbonKg)
                    .sum()
            )
        );
        return summary;
    }

    @Override
    public Map<String, Object> account(String operatorPhone) {
        return summary(operatorPhone);
    }

    @Override
    public List<CarbonRecord> records(String operatorPhone) {
        return carbonRecordRepository.findAllByUserIdOrderByBizDateDescIdDesc(findUser(operatorPhone).getId());
    }

    @Override
    public byte[] certificate(String operatorPhone) {
        User user = findUser(operatorPhone);
        List<CarbonRecord> records = carbonRecordRepository.findAllByUserIdOrderByBizDateDescIdDesc(user.getId());
        int balance = carbonAccountRepository.findByUserId(user.getId())
            .map(account -> account.getBalance() == null ? 0 : account.getBalance())
            .orElse(0);
        double totalCarbonKg = roundToTwo(
            records.stream()
                .filter(record -> record != null && isIncome(record.getType()))
                .mapToDouble(this::extractCarbonKg)
                .sum()
        );

        BufferedImage image = new BufferedImage(1600, 960, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        graphics.setPaint(new GradientPaint(0, 0, new Color(15, 61, 36), 1600, 960, new Color(46, 158, 91)));
        graphics.fillRect(0, 0, 1600, 960);

        graphics.setColor(new Color(255, 255, 255, 30));
        graphics.fillOval(1160, 60, 240, 240);
        graphics.fillOval(80, 620, 320, 320);

        graphics.setColor(Color.WHITE);
        graphics.setFont(new Font("Microsoft YaHei", Font.BOLD, 64));
        graphics.drawString("尚有新生 · 绿色碳减排证书", 100, 160);
        graphics.setFont(new Font("Microsoft YaHei", Font.PLAIN, 28));
        graphics.drawString("Shang You Xin Sheng Carbon Account Certificate", 100, 212);

        graphics.setColor(new Color(255, 255, 255, 242));
        graphics.fillRoundRect(100, 280, 1400, 430, 36, 36);

        graphics.setColor(new Color(15, 23, 42));
        graphics.setFont(new Font("Microsoft YaHei", Font.BOLD, 44));
        graphics.drawString("用户：" + safeText(user.getNickname(), user.getPhone()), 160, 380);
        graphics.drawString("账户等级：" + resolveLevel(balance), 160, 460);
        graphics.drawString("累计积分：" + balance, 160, 540);
        graphics.drawString("累计减碳：" + totalCarbonKg + " kgCO2e", 160, 620);

        graphics.setFont(new Font("Microsoft YaHei", Font.PLAIN, 28));
        graphics.setColor(new Color(51, 65, 85));
        graphics.drawString(
            "平台确认该用户已持续参与绿色循环交易，并形成可量化的碳减排贡献。",
            160,
            690
        );

        graphics.setColor(Color.WHITE);
        graphics.drawString("生成日期：" + java.time.LocalDate.now(), 100, 840);
        graphics.drawString("证书用途：个人留存 / 活动展示 / 绿色消费记录", 100, 888);
        graphics.dispose();

        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            ImageIO.write(image, "png", output);
            byte[] bytes = output.toByteArray();
            persistCertificate(user, bytes);
            return bytes;
        } catch (IOException exception) {
            throw new BusinessException("Failed to generate carbon certificate");
        }
    }

    private User findUser(String phone) {
        return userRepository.findByPhone(phone)
            .orElseThrow(() -> new BusinessException("User not found"));
    }

    private double extractCarbonKg(CarbonRecord record) {
        if (record == null) {
            return 0D;
        }
        Matcher matcher = KG_PATTERN.matcher(record.getDescription() == null ? "" : record.getDescription());
        if (matcher.find()) {
            return Double.parseDouble(matcher.group(1));
        }
        int points = record.getPoints() == null ? 0 : record.getPoints();
        return roundToTwo(points / 15D);
    }

    private double roundToTwo(double value) {
        return Math.round(value * 100D) / 100D;
    }

    private boolean isIncome(String type) {
        return "收入".equals(type) || "鏀跺叆".equals(type) || "INCOME".equalsIgnoreCase(type);
    }

    private void persistCertificate(User user, byte[] bytes) {
        try {
            Path certificateDir = Path.of(fileStorageConfig.getUploadDir(), "certificates");
            Files.createDirectories(certificateDir);
            Path target = certificateDir.resolve("carbon-certificate-" + user.getId() + "-" + LocalDate.now() + ".png");
            Files.write(target, bytes, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException exception) {
            throw new BusinessException("Failed to save carbon certificate");
        }
    }

    private String resolveLevel(int balance) {
        if (balance >= 5000) {
            return "Lv.5 绿色合伙人";
        }
        if (balance >= 3500) {
            return "Lv.4 碳账户先锋";
        }
        if (balance >= 2000) {
            return "Lv.3 循环达人";
        }
        if (balance >= 1000) {
            return "Lv.2 低碳买家";
        }
        return "Lv.1 绿色新手";
    }

    private String safeText(String primary, String fallback) {
        String value = primary != null && !primary.isBlank() ? primary : fallback;
        return value == null ? "平台用户" : value;
    }
}
