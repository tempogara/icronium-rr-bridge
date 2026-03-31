package it.icron.icronium.connector.rr.integration;

import it.icron.icronium.connector.rr.AppLogger;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * LIVE uploader ported from ICRONIUM-RR-LIVE.
 */
public class LiveUploader {

    private static final String TARGET_URL = "https://www.icron.it/IcronLiveServices/services/profilo/live/cacheLiveData";
    private static final String TARGET_URL_RESET = "https://www.icron.it/IcronLiveServices/services/profilo/sistema/resetCache";

    private static final String BEARER_TOKEN = "eyJlcGsiOnsia3R5IjoiRUMiLCJjcnYiOiJQLTI1NiIsIngiOiJmcGdyWVFMOGdhaERjYkpPWUlmSWZFckd6QnZYQkw4b2prS3Z4M2lmN1BvIiwieSI6IjVhRzBqMW01UEFVeWRfQ2lDSWZ4X3Ztd2ZBNFRGVWpwbjJmYXowNmh4cUUifSwiY3R5IjoiSldUIiwiZW5jIjoiQTE5MkNCQy1IUzM4NCIsImFsZyI6IkVDREgtRVMrQTEyOEtXIn0.Kctgg5RCACuiwYMBjkHkUwEXCOGpFU4QbP1pjN0IBXOTixhP18dEJ6ZE6G78xbB6gKpnut4tohg.VAAQbGPpniKJrS46MsVymw.6l9r6WWiijtAcU1tM9QWZ_xByrwA6T507tdE9cosiep52weB13Hou13K--FTasmGO3jdgG7FnPJxAe9AfOZhWA6ptbKb_LQ_CVdz-M3uXRbWBDKBzyJ3CR2fTC5QcSIxOroxcb_HYcWJExYZD8SAuj79r1vUVbk-pcZJxqW4rNCW9hwyiRGpTLUtmdz0frsHATqUqBLHgB0n-3N2dqwtXXLhxrU3DTFbcT0qThs2tHoVRvLudsSvgCNdGG__SlIdOcEjfLUMQMeB3PZ-N3USHNLyDjMa-X_zNCxhY_d7_AyzjcmYMfxJMUZx6y24JZUchVNMa6BIuiAbbxNOkTuBUqaixqE1UXXe_DrzlVOfhzDo0zP5wDz0HvH6huSbBcBSm1IJjLsgBrA-n-d0ha_M7vOLrHdrS7AK-lckuwINOIP5Fbn9UYPcqHRY166qJruRTSglpmHyAWb3q3ZjSVtE45UYe2WjJXv45CHCLwReM5sojtT9f_09t4L-eVuf1Qb21CE2OyxDUBNiof8ZR8MgSy1otmHEE75VHFaa9DoR_s2QGRxx8xhdN-qUvTbnYkSn3HQLplyDETN6emsvi-UL5xTTFVEqtzEMjytUD0VVgTVtqfMSXcGJRyzy0ZWWXzBeTOiq1wqVqa0mhJuYuSGcZcuqerb9Q7-ZrUULdI2et8qucKy55vcP9lLLFavIKTwOquhYEceBePV4fw1HM4zc_bQECBV2ArrRxhcuX2-qmKe3FQe22aNYl96uQA3Wb1md62qAHekwcotWYPBawix-NkWxUN0qqNe3o_E20k9KvPdcncjy0TpqUSgvZPmQKE6fleXOPLZduCP4Q_D1UONGIB4WF6SaG2GutWRTWeV6rTDDUbUOFLOpuYfQDscaxM3ESfAPKp7ZINVBykXBjg2gY8dMwXR_eFAXB_dgIYdaK0BqqY6wzPMwO0KffNETK8w3o7HIRFjJwHm5lqyDWfrmZ02CwBSzGijrZ_kLhoaZFr05DBlccHEikORJm9cnjg1PZM2Nq8E91fvUoLY8RrtucdeNNNPPgStr0Qh0iUg2lcZE1_NwJzzlVH3qnOC2TZO_PvbJF40izduAUqx97Xn4duIi9hJgRDQNA3QMqCqj5L1j3MWfApiD3sxaaddz0bc-P_GarwVQ3Ygu9T-i_LhQv3F7R4ni6ax54-TfSQ247C5LnCCzC6sbRLxPAI3DwaVjLfwaqyG7E7AWEgu9PbJ5N7q7R4lWx3StdC8etzhxHv23yCQd2RuiqV3wzAYym3y8AY_tmh8tIHI7ElfkPUNMy1Q_kxWhPQmoiJ_AWuI6nRm5fs_tgBr4lovESF6LZlk92Jh7yvOCQpSq2n2VDtw-mHzj5jxUGKOiQ58qyLA9MmkE9zVtMb55oR-9T71eTsWcdRtz21mRLAx8uGX8Yj2LIa17GFcKQd6YDh4BU9ylm1-7uSwYaaIvukRRs_4Zce8zQBRu2q2ZMtxtZGnpJm4-X2UZIixRVrtUAIFHb_TPRsvNyqpYFIdVnZGQYWuHKP0BMbsJWrB20oacF07ZTC4x2D3OPC01ODq96ZJKZ7TxsLnOT5x0kuOkVxMPukZFPyQ-frYWuHaAp8Lpez06v-Zvb7pfuA_UDo9wCyS-N_T0JcMh0qjYTYXYLY4dT1_MRlt0hHGtV2e0mwfmZUPW-UnycpNypUIY6mDo9uEaML-C6Hrf9voi5L0iroEwmqDNs7BhD70GiNrapPIMh19oY7yLYLPCR8NRhmO7T2NniPO8BoeG8BtuE2xiMEz8gV6jYCIj0YDm0YW4Zuqe_1jQ2CNwOC7YTO3G0Ui6DfhAjwpbXV9h8FFVQ0qf3FH5bFB-75WQw3UU_o3B7p1PhNemtW-8Vq045EERyd1aPw0bHui_0I9yudf2mu9d1CqJPdXrIIEbEk5WElDGcjJxqeEejPrOSN6T3wQ1r-8kAi14nLQLkuqUJBToMTvTm208mUFWQHHhW7khuceSVPgesUXrfDDY_fP2ZiDiaQMzEHcwN4Qq5BRwJVv86lS7tvpycUh2FvWJWpL_LkKPueGiqOCP5XiTSU56HWss2ARhcTQvMH4sAktfQyOBB5tXxVcEiVkXo6S0I1SLQMie8bARKKJLsU4tuMkJi9rVS2c2rqehwyyO4WhqZBM9rzpuXKn3IHW3yr_aVvBZ1XtJ-Vle8WpvvglhThzAtvLhBoNGh7oiSROryAUugWJkp73-Bl05bgpsZ80kgwC3i1Rrx1v8BZ5llO76lDblp5iYh9j2qy4zXAWAaHWXtS_hSLmFiW3LIcd1SJLeVT5vzjGGxYtc0Ha0Kh47HYHlVdauGITfY1bzzrRGUZlnu-Pat7U7PCtbeAU-AgUHhjhGL51pevkXkAmGInB_T3-3Yfjpe8oW0vKRVRWZUn8QSn0HY7JkUTN4uWNPxV8cbxZxwtr-5PnGmV8hM2DKr3PTTiK40T_ltgsRGH0IImfZV7kT_tsVorBXahYl50kMxAk9mMC9-XJ9K87H5oazKt-rrm4-hOVJbUPJK9NOKTfDaFRYMlrqQ2fMGEtAtL-zhj7ASmuKtDQjV4eXqzqrDys1FE-rWCXYiE4h8Dyu72fWMYgLED25xdvel-V-V2ysf5ZKR5o8vMoVAflBUk24XwUTql6yskZ066kkRfGJx2i83LxChEae9wXKMTqHA3ICcNaah3O-DfDdQVwliQ_jcJzTYXGF5klaXhufd1lQZVDcMWTWW6AOgvquNTLg6_hMOsFrCVY8DBeY80KtJWJboMSzvBPzqX1jbAUdzHIcKPWQYP2L_k65FJjo0p9UUIqRCH4brt1feOA9XFSN3pmvZuDesuiLBScNAHysM0rW_GD20mnm5y7Jcqkl_CG6HkI0bF2DsbYKCFR40vXTHs9zXi-aR3oE_69cDedk4lwjWmeOSFqFzH1vCTPFL_a61bApQKhoydw7vTh3KQCpxaAKTB4LO91S1aL6Ax-3uK6TftEU7nM-NzlUPE6JJiTy_yzqEXPhjc-sAfmzKQ0dd6sMk6vFApIAp2zeAq8olTNxgskoARSWEJ3Tb-oulYJ2dRLPusT39AR2dHzc01vm8DnEF5ANmU9cof3Z8bMispQB41zNzSW1unn5QEdzCGrrAiDdGBq5N6Ayx4XFfqeNf53oM0ylxhsFK2rHESZdHkvnxBV1Edn95ZuUMcc6k8_o1sGY.k3c1uNxdpP6d-v-yQECa96hEI_i81bL8";

    private static final DateTimeFormatter TOKEN_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd-HH:mm:ss");

    private LiveUploader() {
    }

    public static void send(String rowName, byte[] data, String eventId) throws Exception {
        if (data == null) {
            return;
        }
        String dataId = eventId + "_" + (rowName == null ? "" : rowName);
        if (dataId.isBlank()) {
            throw new IllegalArgumentException("dataId vuoto");
        }
        String content = new String(data, StandardCharsets.UTF_8);
        String token = LocalDateTime.now().format(TOKEN_FMT);
        String json = buildJson(dataId, content, token);
        AppLogger.log("LIVE POST -> " + dataId + " (" + data.length + " bytes)");
        post(TARGET_URL, json);
    }

    public static void resetCache(String rowName, String eventId) throws Exception {
        String dataId = eventId + "_" + (rowName == null ? "" : rowName);
        if (dataId.isBlank()) {
            throw new IllegalArgumentException("dataId vuoto");
        }
        String json = buildJsonReset(dataId);
        AppLogger.log("LIVE POST -> " + dataId + " reset");
        post(TARGET_URL_RESET, json);
    }

    private static void post(String targetUrl, String json) throws Exception {
        byte[] body = json.getBytes(StandardCharsets.UTF_8);
        HttpURLConnection conn = (HttpURLConnection) new URL(targetUrl).openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
        conn.setRequestProperty("Authorization", "Bearer " + BEARER_TOKEN);
        conn.setConnectTimeout(8000);
        conn.setReadTimeout(8000);
        conn.setDoOutput(true);
        try (OutputStream os = conn.getOutputStream()) {
            os.write(body);
        }

        int code = conn.getResponseCode();
        if (code < 200 || code >= 300) {
            String err = readError(conn);
            throw new RuntimeException("HTTP " + code + " -> " + err);
        }
        conn.disconnect();
    }

    private static String buildJson(String dataId, String content, String token) {
        return "{"
                + "\"dataId\":\"" + esc(dataId) + "\","
                + "\"data\":\"" + esc(content) + "\","
                + "\"token\":\"" + token + "\""
                + "}";
    }

    private static String buildJsonReset(String dataId) {
        return "{"
                + "\"objectKey\":\"" + esc(dataId) + "\","
                + "\"function\":\"getLiveData\","
                + "\"propaga\":true"
                + "}";
    }

    private static String esc(String s) {
        if (s == null) {
            return "";
        }
        return s
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "");
    }

    private static String readError(HttpURLConnection conn) {
        try (InputStream is = conn.getErrorStream()) {
            if (is == null) {
                return "";
            }
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        } catch (Exception ex) {
            return "";
        }
    }
}
