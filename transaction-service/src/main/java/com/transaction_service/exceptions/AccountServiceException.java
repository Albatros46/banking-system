package com.transaction_service.exceptions;

import feign.FeignException;
import lombok.Getter;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

/*
    account-service'ten donen hata. Durum kodu ve "detail" mesaji istemciye aynen iletilir.
 */
@Getter
public class AccountServiceException extends RuntimeException {

    private static final ObjectMapper MAPPER = JsonMapper.builder().build();

    private final int status;

    public AccountServiceException(int status, String message) {
        super(message);
        this.status = status;
    }

    public static AccountServiceException from(FeignException ex) {
        // status <= 0 -> baglanti hatasi / timeout, account-service'e ulasilamadi
        if (ex.status() <= 0) {
            return new AccountServiceException(503, "Der Kontodienst ist derzeit nicht erreichbar.");
        }
        return new AccountServiceException(ex.status(), extractDetail(ex));
    }

    private static String extractDetail(FeignException ex) {
        try {
            JsonNode detail = MAPPER.readTree(ex.contentUTF8()).get("detail");
            if (detail != null && !detail.isNull()) {
                return detail.asString();
            }
        } catch (RuntimeException ignored) {
            // govde JSON degil
        }
        return "Fehler im Kontodienst (" + ex.status() + ")";
    }
}
