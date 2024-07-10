package org.owasp.benchmarkutils.score.parsers;

import org.json.JSONArray;
import org.json.JSONObject;
import org.owasp.benchmarkutils.score.*;

public class GitLabSastReader extends Reader {
    @Override
    public boolean canRead(ResultFile resultFile) {
        return resultFile.isJson()
                && resultFile.json().has("scan")
                && resultFile
                .json()
                .getJSONObject("scan")
                .getJSONObject("analyzer")
                .getJSONObject("vendor")
                .getString("name")
                .equalsIgnoreCase("GitLab");
    }

    @Override
    public TestSuiteResults parse(ResultFile resultFile) throws Exception {
        TestSuiteResults tr = new TestSuiteResults("GitLab-SAST", true, TestSuiteResults.ToolType.SAST);

        JSONArray vulnerabilities = resultFile.json().getJSONArray("vulnerabilities");

        for (int vulnerability = 0; vulnerability < vulnerabilities.length(); vulnerability++) {
            TestCaseResult tcr = parseGitLabSastFindings(vulnerabilities.getJSONObject(vulnerability));
            if (tcr != null) {
                tr.put(tcr);
            }
        }
        return tr;
    }

    private TestCaseResult parseGitLabSastFindings(JSONObject vulnerability) {

        try {
            String className = vulnerability.getJSONObject("location").getString("file");
            className = (className.substring(className.lastIndexOf('/') + 1)).split("\\.")[0];

            if (className.startsWith(BenchmarkScore.TESTCASENAME)) {
                TestCaseResult tcr = new TestCaseResult();

                JSONArray identifiers = vulnerability.getJSONArray("identifiers");

                int cwe = identifiers.getJSONObject(1).getInt("value");
                cwe = translate(cwe);

                String category = identifiers.getJSONObject(2).getString("name");
                category = category.split("-")[1].strip();

                String evidence = vulnerability.getString("cve");

                tcr.setCWE(cwe);
                tcr.setCategory(category);
                tcr.setEvidence(evidence);
                tcr.setConfidence(0);
                tcr.setNumber(testNumber(className));

                return tcr;
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return null;
    }

    private int translate(int cwe) {

        switch (cwe) {
            case 22:
                return CweNumber.PATH_TRAVERSAL;
            case 79:
                return CweNumber.XSS;
            case 89:
                return CweNumber.SQL_INJECTION;
            case 90:
                return CweNumber.LDAP_INJECTION;
            case 113:
                return CweNumber.HTTP_RESPONSE_SPLITTING;
            case 185:
                return CweNumber.COMMAND_INJECTION;
            case 326:
            case 327:
            case 328:
                return CweNumber.WEAK_CRYPTO_ALGO;
            case 338:
                return CweNumber.WEAK_RANDOM;
            case 614:
                return CweNumber.INSECURE_COOKIE;
            case 643:
                return CweNumber.XPATH_INJECTION;
            case 1004:
                return CweNumber.COOKIE_WITHOUT_HTTPONLY;
            case 259:
            case 306:
                break;
            default:
                System.out.println(
                        "INFO: Found following CWE in GitLab SAST results which we haven't seen before: "
                                + cwe);
        }

        return cwe;
    }
}
