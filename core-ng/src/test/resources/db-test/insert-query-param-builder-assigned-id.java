public class InsertQuery$AssignedIdEntity$ParamBuilder implements java.util.function.Function {
    public Object apply(Object value) {
        core.framework.internal.db.AssignedIdEntity entity = (core.framework.internal.db.AssignedIdEntity) value;
        if (entity.id == null) throw new Error("primary key must not be null, field=id");
        Object[] params = new Object[5];
        params[0] = entity.id;
        params[1] = entity.stringField;
        params[2] = entity.intField;
        params[3] = entity.bigDecimalField;
        params[4] = entity.dateField;
        return params;
    }

}
