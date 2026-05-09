package com.gyvex.pvpindex.factions.data.repository;

import com.github.ezframework.jaloquent.exception.StorageException;
import com.github.ezframework.jaloquent.model.ModelRepository;
import com.github.ezframework.jaloquent.model.TableRegistry;
import com.github.ezframework.jaloquent.store.sql.DataSourceJdbcStore;
import com.github.ezframework.javaquerybuilder.query.builder.QueryBuilder;
import com.gyvex.pvpindex.factions.data.model.BankTransactionModel;
import java.util.Comparator;
import java.util.List;

/** Repository for faction bank transaction records. */
public class BankTransactionRepository extends ModelRepository<BankTransactionModel> {

    public BankTransactionRepository(final DataSourceJdbcStore store) {
        super(store, BankTransactionModel.PREFIX, (id, data) -> new BankTransactionModel(id));
        TableRegistry.register(BankTransactionModel.PREFIX, "bank_transactions", BankTransactionModel.COLUMNS);
    }

    public List<BankTransactionModel> findRecentByFactionId(
            final String factionId, final int limit, final int offset) throws StorageException {
        final List<BankTransactionModel> rows = query(
            new QueryBuilder().whereEquals("faction_id", factionId).build()
        );
        rows.sort(Comparator.comparingLong(BankTransactionModel::getCreatedAt).reversed());
        if (offset >= rows.size()) {
            return List.of();
        }
        final int to = Math.min(rows.size(), offset + limit);
        return rows.subList(offset, to);
    }
}

