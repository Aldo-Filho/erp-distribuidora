import useBarcodeScanner from "../../hooks/useBarcodeScanner";
export default function BarcodeScanner({onResolved,disabled=false}) {
 const scan=useBarcodeScanner(onResolved,disabled);
 return <div className="barcode-scanner"><label>Código de barras<input aria-label="Código de barras" autoComplete="off" spellCheck={false} value={scan.code}
  disabled={disabled} readOnly={scan.pending} onChange={e=>scan.setCode(e.target.value)} placeholder="Leia o código e pressione Enter"
  onKeyDown={e=>{if(e.key==="Enter"){e.preventDefault();e.stopPropagation();scan.resolve();}}}/></label>
  <button type="button" className="secondary" disabled={disabled||scan.pending||!scan.code.trim()} onClick={scan.resolve}>{scan.pending?"Localizando…":"Localizar código"}</button>
  {scan.error&&<p role="alert" className="error">{scan.error}</p>}</div>;
}
