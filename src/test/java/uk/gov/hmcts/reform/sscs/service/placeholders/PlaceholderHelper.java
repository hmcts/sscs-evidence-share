package uk.gov.hmcts.reform.sscs.service.placeholders;

import uk.gov.hmcts.reform.sscs.ccd.domain.Address;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appeal;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appellant;
import uk.gov.hmcts.reform.sscs.ccd.domain.BenefitType;
import uk.gov.hmcts.reform.sscs.ccd.domain.Identity;
import uk.gov.hmcts.reform.sscs.ccd.domain.MrnDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.Name;
import uk.gov.hmcts.reform.sscs.ccd.domain.RegionalProcessingCenter;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;

final class PlaceholderHelper {

    private PlaceholderHelper() {
    }

    private static final String RPC_ADDRESS1 = "HM Courts & Tribunals Service";
    private static final String RPC_ADDRESS2 = "Social Security & Child Support Appeals";
    private static final String RPC_ADDRESS3 = "Prudential Buildings";
    private static final String RPC_ADDRESS4 = "36 Dale Street";
    private static final String RPC_CITY = "LIVERPOOL";
    private static final String POSTCODE = "L2 5UZ";


    static SscsCaseData buildCaseData() {

        RegionalProcessingCenter rpc = RegionalProcessingCenter.builder()
            .name("Liverpool").address1(RPC_ADDRESS1).address2(RPC_ADDRESS2).address3(RPC_ADDRESS3)
            .address4(RPC_ADDRESS4).city(RPC_CITY).postcode(POSTCODE).build();

        return SscsCaseData.builder()
            .ccdCaseId("123456")
            .regionalProcessingCenter(rpc)
            .appeal(Appeal.builder()
                .mrnDetails(MrnDetails.builder().dwpIssuingOffice("1").build())
                .benefitType(BenefitType.builder().code("PIP").description("Personal Independence Payment").build())
                .appellant(Appellant.builder()
                    .name(Name.builder().title("Mr").firstName("Terry").lastName("Tibbs").build())
                    .identity(Identity.builder().nino("JT0123456B").build())
                    .address(Address.builder()
                        .line1("HM Courts & Tribunals Service")
                        .line2("Down the road")
                        .town("Social Security & Child Support Appeals")
                        .county("Prudential Buildings")
                        .postcode("L2 5UZ")
                        .build())
                    .build())
                .build())
            .build();
    }
}
