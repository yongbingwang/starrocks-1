[sql]
select
    ps_partkey,
    sum(ps_supplycost * ps_availqty) as value
from
    partsupp,
    supplier,
    nation
where
    ps_suppkey = s_suppkey
  and s_nationkey = n_nationkey
  and n_name = 'PERU'
group by
    ps_partkey having
    sum(ps_supplycost * ps_availqty) > (
    select
    sum(ps_supplycost * ps_availqty) * 0.0001000000
    from
    partsupp,
    supplier,
    nation
    where
    ps_suppkey = s_suppkey
                  and s_nationkey = n_nationkey
                  and n_name = 'PERU'
    )
order by
    value desc ;
[result]
TOP-N (order by [[18: sum DESC NULLS LAST]])
    TOP-N (order by [[18: sum DESC NULLS LAST]])
        INNER JOIN (join-predicate [cast(18: sum as double) > cast(37: expr as double)] post-join-predicate [null])
            AGGREGATE ([GLOBAL] aggregate [{18: sum=sum(18: sum)}] group by [[1: ps_partkey]] having [null]
                EXCHANGE SHUFFLE[1]
                    AGGREGATE ([LOCAL] aggregate [{18: sum=sum(17: expr)}] group by [[1: ps_partkey]] having [null]
<<<<<<< HEAD
                        SCAN (mv[partsupp_mv] columns[48: n_name, 52: ps_partkey, 62: ps_partvalue] predicate[48: n_name = PERU])
=======
                        SCAN (mv[partsupp_mv] columns[44: n_name, 48: ps_partkey, 58: ps_partvalue] predicate[44: n_name = PERU])
>>>>>>> 2.5.18
            EXCHANGE BROADCAST
                ASSERT LE 1
                    AGGREGATE ([GLOBAL] aggregate [{36: sum=sum(36: sum)}] group by [[]] having [null]
                        EXCHANGE GATHER
                            AGGREGATE ([LOCAL] aggregate [{36: sum=sum(35: expr)}] group by [[]] having [null]
<<<<<<< HEAD
                                SCAN (mv[partsupp_mv] columns[48: n_name, 62: ps_partvalue] predicate[48: n_name = PERU])
=======
                                SCAN (mv[partsupp_mv] columns[114: n_name, 128: ps_partvalue] predicate[114: n_name = PERU])
>>>>>>> 2.5.18
[end]

