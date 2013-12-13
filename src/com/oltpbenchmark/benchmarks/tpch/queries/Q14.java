package com.oltpbenchmark.benchmarks.tpch.queries;

import com.oltpbenchmark.api.SQLStmt;

public class Q14 extends GenericQuery {

    public final SQLStmt query_stmt = new SQLStmt(
              "select "
            +     "100.00 * sum(case "
            +         "when p_type like 'PROMO%' "
            +             "then l_extendedprice * (1 - l_discount) "
            +         "else 0 "
            +     "end) / sum(l_extendedprice * (1 - l_discount)) as promo_revenue "
            + "from "
            +     "lineitem, "
            +     "part "
            + "where "
            +     "l_partkey = p_partkey "
            +     "and l_shipdate >= date '1997-04-01' "
            +     "and l_shipdate < date '1997-04-01' + interval '1' month"
        );

    protected SQLStmt get_query() {
        return query_stmt;
    }
}
