package com.educria.config;

import org.hibernate.boot.model.FunctionContributions;
import org.hibernate.boot.model.FunctionContributor;
import org.hibernate.type.StandardBasicTypes;

public class FuncoesPersistencia implements FunctionContributor {
    // Conjunto de Character.isWhitespace usado por String.isBlank em Java 21.
    private static final String ESPACOS = " \t\n\013\f\r\u001C\u001D\u001E\u001F\u1680"
            + "\u2000\u2001\u2002\u2003\u2004\u2005\u2006\u2008\u2009\u200A\u2028\u2029\u205F\u3000";

    @Override
    public void contributeFunctions(FunctionContributions contribuicoes) {
        // T-SQL exige alfabeto de reposição do mesmo tamanho; comparação binária mantém NBSP como texto.
        String padrao = "datalength(replace(translate(cast(?1 as nvarchar(max)) collate Latin1_General_100_BIN2, N'"
                + ESPACOS + "', N'" + " ".repeat(ESPACOS.length()) + "'), N' ', N''))";
        contribuicoes.getFunctionRegistry().registerPattern("educria_tamanho_texto", padrao,
                contribuicoes.getTypeConfiguration().getBasicTypeRegistry().resolve(StandardBasicTypes.LONG));
    }
}
