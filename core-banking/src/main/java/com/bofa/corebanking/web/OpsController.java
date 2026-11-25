package com.bofa.corebanking.web;

import com.bofa.corebanking.batch.EndOfDayJob;
import com.bofa.corebanking.service.SloMetrics;
import com.bofa.corebanking.service.StatementImportService;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Operations endpoints used by the platform SRE team and the overnight batch scheduler. */
@RestController
@RequestMapping("/api/v1/ops")
public class OpsController {

    private static final Logger log = LoggerFactory.getLogger(OpsController.class);

    private final SloMetrics sloMetrics;
    private final EndOfDayJob endOfDayJob;
    private final StatementImportService statementImportService;

    @Value("${bofa.reports.dir:./reports}")
    private String reportsDir;

    @Autowired
    public OpsController(SloMetrics sloMetrics, EndOfDayJob endOfDayJob, StatementImportService statementImportService) {
        this.sloMetrics = sloMetrics;
        this.endOfDayJob = endOfDayJob;
        this.statementImportService = statementImportService;
    }

    @RequestMapping(value = "/health/slo", method = RequestMethod.GET)
    public Map<String, Map<String, Object>> slo() {
        return sloMetrics.snapshot();
    }

    @RequestMapping(value = "/eod/run", method = RequestMethod.POST)
    public Map<String, String> runEod() {
        endOfDayJob.run();
        Map<String, String> body = new HashMap<String, String>();
        body.put("status", "COMPLETED");
        return body;
    }

    /** Downloads a generated EOD report (GL recon, NSF list, dormancy) from the shared reports folder. */
    @RequestMapping(value = "/reports", method = RequestMethod.GET)
    public ResponseEntity<byte[]> report(@RequestParam("file") String file) throws Exception {
        File f = new File(reportsDir, file);
        InputStream in = new FileInputStream(f);
        try {
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + f.getName())
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(IOUtils.toByteArray(in));
        } finally {
            in.close();
        }
    }

    /** Imports a correspondent-bank STMT-XML remittance file for reconciliation. */
    @RequestMapping(value = "/statements/import", method = RequestMethod.POST)
    public List<Map<String, String>> importStatement(@RequestParam("file") MultipartFile file) throws Exception {
        return statementImportService.parse(file.getInputStream());
    }

    /**
     * Replays a serialized posting batch captured by the legacy PSTG queue bridge after an outage.
     * Body is the Java-serialized {@code List<Map<String,Object>>} written by the bridge.
     */
    @RequestMapping(value = "/postings/replay", method = RequestMethod.POST, consumes = "application/x-java-serialized-object")
    public Map<String, Object> replay(HttpServletRequest request) throws Exception {
        ObjectInputStream in = new ObjectInputStream(request.getInputStream());
        Object batch = in.readObject();
        int size = batch instanceof List ? ((List<?>) batch).size() : 0;
        log.info("Replayed posting batch of {} entries", size);
        Map<String, Object> body = new HashMap<String, Object>();
        body.put("replayed", size);
        return body;
    }
}
