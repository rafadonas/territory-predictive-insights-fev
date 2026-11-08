package servico;

import dominio.Medicao;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

/**
 * BÔNUS: exporta o histórico consultado para CSV.
 * Usa ";" como separador e vírgula decimal, que é o que o Excel em português espera,
 * e grava o BOM UTF-8 para os acentos aparecerem certos.
 */
public final class ExportadorCsv {

    private static final DateTimeFormatter DATA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
    private static final DateTimeFormatter NOME_ARQUIVO = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    private ExportadorCsv() {
    }

    public static Path exportarHistorico(String sensorId, List<Medicao> medicoes, Path pasta) throws IOException {
        Files.createDirectories(pasta);
        Path arquivo = pasta.resolve("historico_" + sensorId + "_" + LocalDateTime.now().format(NOME_ARQUIVO) + ".csv");
        try (BufferedWriter saida = Files.newBufferedWriter(arquivo, StandardCharsets.UTF_8)) {
            saida.write('﻿');
            saida.write("id;sensor;tipo;regiao;valor;unidade;data_hora");
            saida.newLine();
            for (Medicao m : medicoes) {
                saida.write(String.join(";",
                        String.valueOf(m.getId()),
                        m.getSensorId(),
                        m.getTipo().getGrandeza(),
                        m.getRegiao(),
                        String.format(Locale.forLanguageTag("pt-BR"), "%.1f", m.getValor()),
                        m.getTipo().getUnidade(),
                        m.getDataHora().format(DATA_HORA)));
                saida.newLine();
            }
        }
        return arquivo;
    }
}
