package uk.gov.hmcts.reform.sscs.domain;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import org.json.simple.JSONObject;


public class BenefitLookup<T> extends ConcurrentHashMap<T, DwpAddress> {
    private static final long serialVersionUID = -7017349940075567843L;
    private static final String CODE = "code";

    private static final String ADDRESS = "address";
    private static final String LINE_1 = "line1";
    private static final String LINE_2 = "line2";
    private static final String LINE_3 = "line3";
    private static final String POST_CODE = "postCode";

    public BenefitLookup(List<JSONObject> jsonArray) {
        super();
        jsonArray.forEach(jsonObj -> {
            @SuppressWarnings("unchecked")
            T code = (T) jsonObj.get(CODE);
            this.put(code, getAddress((JSONObject) jsonObj.get(ADDRESS)));
        });
    }

    public static DwpAddress getAddress(JSONObject jsonObj) {
        String line1 = (String) jsonObj.get(LINE_1);
        String line2 = (String) jsonObj.get(LINE_2);
        String line3 = (String) jsonObj.get(LINE_3);
        String postCode = (String) jsonObj.get(POST_CODE);
        return new DwpAddress(line1, line2, line3, postCode);
    }
}
