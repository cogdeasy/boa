package com.bofa.statements.web;

import com.bofa.common.util.BankDates;
import com.bofa.statements.client.CoreBankingClient;
import com.bofa.statements.render.StatementPdfRenderer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/statements")
public class StatementController {

    private final CoreBankingClient coreBanking;
    private final StatementPdfRenderer renderer;

    @Autowired
    public StatementController(CoreBankingClient coreBanking, StatementPdfRenderer renderer) {
        this.coreBanking = coreBanking;
        this.renderer = renderer;
    }

    @RequestMapping(value = "/{accountNumber}", method = RequestMethod.GET)
    public Map<String, Object> statement(@PathVariable String accountNumber,
                                         @RequestParam("month") String month) {
        return build(accountNumber, month);
    }

    @RequestMapping(value = "/{accountNumber}/pdf", method = RequestMethod.GET)
    public ResponseEntity<byte[]> pdf(@PathVariable String accountNumber, @RequestParam("month") String month) {
        byte[] pdf = renderer.render(build(accountNumber, month));
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=statement-" + month + ".pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    private Map<String, Object> build(String accountNumber, String month) {
        Date from = BankDates.parseDate(month + "-01");
        Calendar cal = Calendar.getInstance(BankDates.BANK_TZ);
        cal.setTime(from);
        cal.add(Calendar.MONTH, 1);
        Date to = BankDates.addDays(cal.getTime(), -1);

        Map<String, Object> account = coreBanking.getAccount(accountNumber);
        List<Map<String, Object>> lines = coreBanking.getTransactions(accountNumber, from, to);

        BigDecimal closing = new BigDecimal(String.valueOf(account.get("ledgerBalance")));
        BigDecimal movement = BigDecimal.ZERO;
        for (Map<String, Object> line : lines) {
            movement = movement.add(new BigDecimal(String.valueOf(line.get("amount"))));
        }

        Map<String, Object> statement = new LinkedHashMap<String, Object>();
        statement.put("accountNumber", accountNumber);
        statement.put("maskedNumber", account.get("maskedNumber"));
        statement.put("nickname", account.get("nickname"));
        statement.put("period", month);
        statement.put("openingBalance", closing.subtract(movement).toPlainString());
        statement.put("closingBalance", closing.toPlainString());
        statement.put("lines", lines);
        return statement;
    }
}
