package ledger.scenario;

import static ledger.core.Currency.AED;
import static ledger.core.Currency.BHD;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import ledger.core.Account;
import org.junit.jupiter.api.Test;

class BriefScenarioTest {

    @Test
    void br1_theBriefHasTwoAccounts_ACC001InAed_ACC002InBhd() {
        assertEquals(List.of(new Account("ACC-001", AED), new Account("ACC-002", BHD)), BriefScenario.accounts());
    }
}
