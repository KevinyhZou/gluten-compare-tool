package io.glutenproject.sql.compare;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
public class CompareController {

    @Autowired
    private CompareService compareService;

    @RequestMapping(value = "/compare/single", method = RequestMethod.POST)
    @ResponseBody
    public SingleSqlCompareResponse compare(@RequestBody SingleSqlCompareRequest request) {
        try {
            String resultMsg = compareService.compareSql(null, request.getUser(), request.getSql());
            return new SingleSqlCompareResponse(0, resultMsg);
        } catch (Exception e) {
            return new SingleSqlCompareResponse(-1, e.getMessage());
        }
    }

    @RequestMapping(value = "/compare/batch", method = RequestMethod.POST)
    @ResponseBody
    public BatchSqlCompareResponse compare(@RequestBody BatchSqlCompareRequest request) {
        try {
            String resultMsg = compareService.compareBatch(request.getUser(), request.getSqlPath());
            return new BatchSqlCompareResponse(0, resultMsg);
        } catch (Exception e) {
            return new BatchSqlCompareResponse(-1, e.getMessage());
        }
    }

    @RequestMapping(value = "/compare/session/close", method = RequestMethod.POST)
    @ResponseBody
    public String closeSession(String sessionId, Boolean glutenSession) {
        try {
            compareService.closeSession(sessionId, glutenSession);
            return "OK";
        } catch (Exception e) {
            return e.getMessage();
        }
    }

    @RequestMapping(value = "/compare/test", method = RequestMethod.GET)
    public String test() {
        return compareService.test();
    }
}
