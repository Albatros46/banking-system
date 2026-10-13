package com.transaction_service.enums;
/*
    PENDING   -> gonderenden dusuldu, fraud kontrolu bekleniyor
    COMPLETED -> aliciya yatirildi
    FAILED    -> basarisiz, dusulen tutar gonderene iade edildi
 */
public enum TransactionStatus {
    PENDING,
    COMPLETED,
    FAILED
}
