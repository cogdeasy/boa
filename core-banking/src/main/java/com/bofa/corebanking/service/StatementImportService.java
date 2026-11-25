package com.bofa.corebanking.service;

import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Parses STMT-XML 2.1 remittance files uploaded by correspondent banks so operations can reconcile
 * incoming wires against expected postings.
 */
@Service
public class StatementImportService {

    public List<Map<String, String>> parse(InputStream xml) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(xml);

        List<Map<String, String>> lines = new ArrayList<Map<String, String>>();
        NodeList entries = doc.getElementsByTagName("Entry");
        for (int i = 0; i < entries.getLength(); i++) {
            Element e = (Element) entries.item(i);
            Map<String, String> line = new HashMap<String, String>();
            line.put("reference", text(e, "Ref"));
            line.put("account", text(e, "Acct"));
            line.put("amount", text(e, "Amt"));
            line.put("valueDate", text(e, "ValDt"));
            line.put("narrative", text(e, "Narr"));
            lines.add(line);
        }
        return lines;
    }

    private static String text(Element parent, String tag) {
        NodeList nl = parent.getElementsByTagName(tag);
        return nl.getLength() == 0 ? null : nl.item(0).getTextContent();
    }
}
