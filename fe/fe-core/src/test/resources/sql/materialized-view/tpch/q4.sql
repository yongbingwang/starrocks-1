[sql]
select
    o_orderpriority,
    count(*) as order_count
from
    orders
where
        o_orderdate >= date '1994-09-01'
  and o_orderdate < date '1994-12-01'
  and exists (
        select
            *
        from
            lineitem
        where
                l_orderkey = o_orderkey
          and l_receiptdate > l_commitdate
    )
group by
    o_orderpriority
order by
    o_orderpriority ;
[result]
TOP-N (order by [[6: o_orderpriority ASC NULLS FIRST]])
    TOP-N (order by [[6: o_orderpriority ASC NULLS FIRST]])
<<<<<<< HEAD
        AGGREGATE ([GLOBAL] aggregate [{119: count=sum(119: count)}] group by [[59: o_orderpriority]] having [null]
            EXCHANGE SHUFFLE[59]
                AGGREGATE ([LOCAL] aggregate [{119: count=sum(60: order_count)}] group by [[59: o_orderpriority]] having [null]
                    SCAN (mv[query4_mv] columns[58: o_orderdate, 59: o_orderpriority, 60: order_count] predicate[58: o_orderdate >= 1994-09-01 AND 58: o_orderdate < 1994-12-01 AND 58: o_orderdate >= 1994-01-01 AND 58: o_orderdate < 1995-01-01])
=======
        AGGREGATE ([GLOBAL] aggregate [{115: count=sum(115: count)}] group by [[58: o_orderpriority]] having [null]
            EXCHANGE SHUFFLE[58]
                AGGREGATE ([LOCAL] aggregate [{115: count=sum(59: order_count)}] group by [[58: o_orderpriority]] having [null]
                    SCAN (mv[query4_mv] columns[57: o_orderdate, 58: o_orderpriority, 59: order_count] predicate[57: o_orderdate >= 1994-09-01 AND 57: o_orderdate < 1994-12-01])
>>>>>>> 2.5.18
[end]

