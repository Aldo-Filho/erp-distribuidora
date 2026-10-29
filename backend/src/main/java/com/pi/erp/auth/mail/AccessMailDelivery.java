package com.pi.erp.auth.mail;

import com.pi.erp.auth.audit.SecurityAuditService;
import com.pi.erp.auth.config.AuthenticationProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.support.TransactionTemplate;

@Component
public class AccessMailDelivery {

    @Autowired
    @Qualifier("accessMailExecutor")
    private TaskExecutor accessMailExecutor;

    @Autowired
    private JavaMailSender mailSender;

    @Autowired
    private AuthenticationProperties properties;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private SecurityAuditService securityAuditService;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void send(AccessMailService.Delivery delivery) {
        try {
            accessMailExecutor.execute(() -> deliver(delivery));
        } catch (TaskRejectedException exception) {
            outcome(delivery, false);
        }
    }

    private void deliver(AccessMailService.Delivery delivery) {
        boolean sent = false;
        try {
            if (
                delivery.text() != null &&
                properties.getMailFrom() != null &&
                !properties.getMailFrom().isBlank()
            ) {
                SimpleMailMessage message = new SimpleMailMessage();
                message.setFrom(properties.getMailFrom());
                message.setTo(delivery.email());
                message.setSubject("ERP Distribuidora — account access");
                message.setText(delivery.text());
                mailSender.send(message);
                sent = true;
            }
        } catch (Exception exception) {
            // SMTP exceptions may include recipients or links. Persist only a sanitized outcome.
            sent = false;
        }
        outcome(delivery, sent);
    }

    private void outcome(AccessMailService.Delivery delivery, boolean delivered) {
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);
        transaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        transaction.executeWithoutResult(status -> {
            if (delivery.invitationId() != null) {
                jdbc.update(
                    "UPDATE user_invitations SET delivery_status = ? WHERE id = ? AND token_hash = ?",
                    delivered ? "SUBMITTED" : "FAILED",
                    delivery.invitationId(),
                    delivery.tokenHash()
                );
            }
            securityAuditService.record(
                "ACCESS_EMAIL",
                delivery.invitationId() == null ? null : delivery.invitationId().toString(),
                delivered ? "SUBMITTED" : "FAILURE"
            );
        });
    }
}
