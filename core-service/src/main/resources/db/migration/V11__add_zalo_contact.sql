ALTER TABLE users
    ADD COLUMN zalo VARCHAR(100);

ALTER TABLE notifications
    DROP CONSTRAINT ck_notifications_type;

ALTER TABLE notifications
    ADD CONSTRAINT ck_notifications_type CHECK (
        type IN (
            'SYSTEM',
            'INVOICE',
            'POST_APPROVAL',
            'INTEREST_REQUEST',
            'CONTACT_CARD',
            'GENERAL'
        )
    );
