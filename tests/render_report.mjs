import fs from "node:fs/promises";
if (!process.env.LUNA_XLSX || !process.env.LUNA_PREVIEW || !process.env.ARTIFACT_TOOL_MODULE) {
  throw new Error("LUNA_XLSX, LUNA_PREVIEW e ARTIFACT_TOOL_MODULE sono obbligatori");
}

const { FileBlob, SpreadsheetFile } = await import(process.env.ARTIFACT_TOOL_MODULE);

const workbook = await SpreadsheetFile.importXlsx(await FileBlob.load(process.env.LUNA_XLSX));
const inspected = await workbook.inspect({
  kind: "table",
  range: "Dashboard!A1:H27",
  include: "values,formulas",
  tableMaxRows: 30,
  tableMaxCols: 12,
  maxChars: 5000,
});
const errors = await workbook.inspect({
  kind: "match",
  searchTerm: "#REF!|#DIV/0!|#VALUE!|#NAME\\?|#N/A",
  options: { useRegex: true, maxResults: 100 },
  summary: "formula errors",
});
const previews = [];
for (const sheetName of ["Dashboard", "Vendite", "Acquisti", "Magazzino", "Commesse", "Tesoreria", "HR", "Controlli"]) {
  const preview = await workbook.render({ sheetName, autoCrop: "all", scale: 1.25, format: "png" });
  const target = process.env.LUNA_PREVIEW.replace(/\.png$/i, `-${sheetName.toLowerCase()}.png`);
  await fs.writeFile(target, new Uint8Array(await preview.arrayBuffer()));
  previews.push(target);
}
console.log(JSON.stringify({ inspection: inspected.ndjson, errors: errors.ndjson, previews }));
