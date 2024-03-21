public class TransportOrdersHandlingUnitsRepository extends TransactionalRepository<ShipUnit> {
    private HibernateUtils hibernate;

    public TransportOrdersHandlingUnitsRepository(){
        hibernate = new HibernateUtils();
        hibernate.setUp(getPersistenceUnitName());
    }

    public List<ShipUnit> findCustomData(Long transportOrdersEventID){
        String hql = "SELECT new ShipUnit(eu.handlingUnitCode, " +
            "eu.eventNumberProgressive, udm.customerLabel1, udm.customerLabel2) " +
            "FROM TransportOrdersEventsHandlingUnits eu" +
            " join TransportOrdersHandlingUnits udm " +
            "on eu.transportOrdersHandlingUnitID = udm.transportOrdersHandlingUnitID " +
            "where eu.transportOrdersEventID = ?1" + 
            "and eu.operationType <> 'd' and udm.operationType <> 'd'";
        return hibernate.findQuery(hql, List.of(transportOrdersEventID), ShipUnit.class);
    }

    public List<ShipUnit> findCustomDataForEventVEM(Long transportOrderID){
        String hql = "SELECT new ShipUnit(eu.handlingUnitCode, " +
            "0, udm.customerLabel1, udm.customerLabel2) " +
            "FROM TransportOrdersHandlingUnitsVerifications eu" + 
            "join TransportOrdersHandlingUnits udm " +
            "on eu.transportOrdersHandlingUnitID = udm.transportOrdersHandlingUnitID " +
            "where eu.transportOrderID = ?1" +
            "and eu.operationType <> 'd' and udm.operationType <> 'd'";
        return hibernate.findQuery(hql, List.of(transportOrderID), ShipUnit.class);
    }

    @Override
    public Class<ShipUnit> getEntityClass() {
        return ShipUnit.class;
    }
}
