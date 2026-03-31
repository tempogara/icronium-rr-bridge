package it.tempogara.batch;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class GeneraGraduatoriaFIDAL {

	private static String IDEVENTO = "20254186";
	private static String DIVISIONE = "21KM";
	private static String LUOGO = "BARLETTA";
	private static String DATAEVENTO = "08/02/2026";

	private static String INPUTFOLDER = "/Users/vitocandela/Documents/atleticapuglia/certificate/";
	private static String OUTPUTFOLDER = "/Users/vitocandela/Documents/atleticapuglia/certificate/";

	private static final Map<String, String> CODICI_GARA_BY_DIVISIONE = new LinkedHashMap<String, String>();

	static {
		addCodiceGara("92", "5KM", "5 KM", "KM5");
		addCodiceGara("18", "10KM", "10 KM", "KM10");
		addCodiceGara("68", "21KM", "21 KM", "HALF", "HALFMARATHON", "MEZZA", "MEZZA MARATONA");
		addCodiceGara("53", "MARATONA", "42KM", "42 KM", "MARATHON");
		addCodiceGara("70", "50KM", "50 KM", "ULTRA50");
		addCodiceGara("71", "100KM", "100 KM", "ULTRA100");
		addCodiceGara("21", "MARCIA5KM", "MARCIA 5KM", "MARCIA 5 KM");
		addCodiceGara("23", "MARCIA6KM", "MARCIA 6KM", "MARCIA 6 KM");
		addCodiceGara("49", "MARCIA10KM", "MARCIA 10KM", "MARCIA 10 KM");
		addCodiceGara("22", "MARCIA15KM", "MARCIA 15KM", "MARCIA 15 KM");
		addCodiceGara("64", "MARCIA20KM", "MARCIA 20KM", "MARCIA 20 KM");
		addCodiceGara("83", "MARCIA30KM", "MARCIA 30KM", "MARCIA 30 KM");
		addCodiceGara("82", "MARCIA35KM", "MARCIA 35KM", "MARCIA 35 KM");
		addCodiceGara("72", "MARCIA50KM", "MARCIA 50KM", "MARCIA 50 KM");
	}

	public static SimpleDateFormat df = new SimpleDateFormat("dd/MM/yyyy");
	public static SimpleDateFormat dfTime = new SimpleDateFormat("HH.mm.ss");

	public static void execute(String idGara, String divisione) throws Exception {
		df.parse(DATAEVENTO);
		List<Map<String, String>> classifica = readClassifica(idGara, divisione);

		File folder = new File(OUTPUTFOLDER);
		if (!folder.exists()) {
			folder.mkdirs();
		}

		String fileName = OUTPUTFOLDER + LUOGO + "_" + DATAEVENTO.replaceAll("/", "") + "_" + IDEVENTO + "_" + divisione + ".csv";
		Path outputPath = Path.of(fileName);

		try (BufferedWriter writer = Files.newBufferedWriter(outputPath, StandardCharsets.UTF_8)) {
			writer.write("CodFedGara;SerieTurni;TipoTurno;NumeroTurno;Cronometraggio;FormatoPrestazione;dataprestazione;NTessera;CodSocGara;CodSocApp;FlagTesserato;NomiAtletiStaff;CategStaff;Piazzamento;TipoClassifica;Prestazione;Vento");
			writer.newLine();

			for (Map<String, String> part : classifica) {
				if (part.get("PETT") == null) {
					continue;
				}

				String categoria = resolveCategoria(part);

				String tessera = normalize(part.get("TESSERA"));
				String codSoc = normalize(part.get("COD.SQUADRA"));
				String cognomeNome = normalize(part.get("COGNOME")) + " " + normalize(part.get("NOME"));
				String tempo = convertiTempo(normalize(part.get("TEMPO")));
				String piazzamento = normalize(part.get("POS"));
				String flagTesserato = tessera.isBlank() ? "N" : "S";

				writeRow(writer,
					getCodiceGara(divisione),
					"S",
					"U",
					"1",
					"E",
					"11",
					DATAEVENTO,
					tessera,
					codSoc,
					codSoc,
					flagTesserato,
					cognomeNome.trim(),
					categoria,
					piazzamento,
					"0",
					tempo,
					"");
			}
		}
	}

	private static String resolveCategoria(Map<String, String> part) throws Exception {
		String categoria = normalize(part.get("CAT"));
		if (FidalCategories.isAllowedCategory(categoria)) {
			return categoria;
		}

		String sesso = firstNonBlank(part, "SESSO", "SEX", "SX");
		String anno = firstNonBlank(part, "ANNO", "ANNO.NASCITA", "ANNO DI NASCITA", "NASCITA", "DATA.NASCITA");
		Integer annoNascita = extractYear(anno);
		if (annoNascita != null) {
			String categoriaCalcolata = FidalCategories.getCategory(sesso, annoNascita, getAnnoEvento());
			if (FidalCategories.isAllowedCategory(categoriaCalcolata)) {
				return categoriaCalcolata;
			}
		}

		throw new Exception("Categoria FIDAL non ammessa o non ricavabile per atleta: " + part);
	}

	private static void writeRow(BufferedWriter writer, String... values) throws IOException {
		for (int i = 0; i < values.length; i++) {
			if (i > 0) {
				writer.write(';');
			}
			writer.write(csv(values[i]));
		}
		writer.newLine();
	}

	private static String csv(String value) {
		if (value == null) {
			return "";
		}
		if (value.indexOf(';') >= 0 || value.indexOf('"') >= 0 || value.indexOf('\n') >= 0 || value.indexOf('\r') >= 0) {
			return "\"" + value.replace("\"", "\"\"") + "\"";
		}
		return value;
	}

	private static String normalize(String value) {
		return value == null ? "" : value.trim();
	}

	private static String getCodiceGara(String divisione) throws Exception {
		String key = normalizeDivisione(divisione);
		String codiceGara = CODICI_GARA_BY_DIVISIONE.get(key);
		if (codiceGara == null) {
			throw new Exception("Divisione non mappata per codice gara FIDAL: " + divisione);
		}
		return codiceGara;
	}

	private static void addCodiceGara(String codice, String... divisioni) {
		for (String divisione : divisioni) {
			CODICI_GARA_BY_DIVISIONE.put(normalizeDivisione(divisione), codice);
		}
	}

	private static String normalizeDivisione(String divisione) {
		return normalize(divisione)
			.toUpperCase()
			.replace(".", "")
			.replace("-", "")
			.replace("_", "")
			.replace(" ", "");
	}

	private static String firstNonBlank(Map<String, String> values, String... keys) {
		for (String key : keys) {
			String value = normalize(values.get(key));
			if (!value.isBlank()) {
				return value;
			}
		}
		return "";
	}

	private static Integer extractYear(String value) {
		String normalized = normalize(value);
		if (normalized.isBlank()) {
			return null;
		}

		if (normalized.matches("\\d{4}")) {
			return Integer.parseInt(normalized);
		}

		if (normalized.matches("\\d{2}/\\d{2}/\\d{4}")) {
			return Integer.parseInt(normalized.substring(normalized.length() - 4));
		}

		if (normalized.matches("\\d{2}-\\d{2}-\\d{4}")) {
			return Integer.parseInt(normalized.substring(normalized.length() - 4));
		}

		return null;
	}

	private static int getAnnoEvento() throws Exception {
		return df.parse(DATAEVENTO).toInstant().atZone(java.time.ZoneId.systemDefault()).getYear();
	}

	private static String convertiTempo(String time) {
		if (time == null || time.isBlank()) {
			return "";
		}

		String[] parts = time.split(":");
		if (parts.length != 3) {
			return time;
		}

		String hh = String.valueOf(Integer.parseInt(parts[0]));
		String mm = parts[1];
		String ss = parts[2];

		if (hh.equals("0")) {
			return mm + ":" + ss;
		}

		return hh + "h" + mm + ":" + ss;
	}

	public static void main(String args[]) throws Exception {
		execute(IDEVENTO, DIVISIONE);
	}

	private static List<Map<String, String>> readClassifica(String idGara, String divisione) throws IOException {
		List<Map<String, String>> classifica = new ArrayList<Map<String, String>>();
		List<String> listaColonne = new ArrayList<String>();

		try (BufferedReader reader = new BufferedReader(new FileReader(INPUTFOLDER + idGara + "_" + divisione + ".csv"))) {
			String line = reader.readLine();
			if (line == null) {
				return classifica;
			}

			line = line.toUpperCase().replaceAll("\"", "");
			String[] tok = line.split(";");
			for (int i = 0; i < tok.length; i++) {
				listaColonne.add(tok[i]);
			}

			line = reader.readLine();
			while (line != null) {
				System.out.println(line);
				line = line.toUpperCase().replaceAll("\"", "").replaceAll("\\.", ":");
				String[] tok2 = line.split(";");
				Map<String, String> m = new HashMap<String, String>();
				for (int i = 0; i < tok2.length && i < listaColonne.size(); i++) {
					m.put(listaColonne.get(i), tok2[i]);
				}

				try {
					Date tsTempo = dfTime.parse(m.get("TEMPO"));
					m.put("TIMEF3", dfTime.format(tsTempo));
				} catch (Exception e) {
				}

				classifica.add(m);
				line = reader.readLine();
			}
		}

		return classifica;
	}
}
