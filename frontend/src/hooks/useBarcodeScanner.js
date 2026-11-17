import { useEffect,useRef,useState } from "react";
import { barcodes } from "../services/barcodes";
// A focused input accepts keyboard-wedge readers, Enter and ordinary typing alike.
export default function useBarcodeScanner(onResolved,disabled=false) {
 const [code,setCode]=useState(""); const [pending,setPending]=useState(false); const [error,setError]=useState("");
 const lock=useRef(false); const request=useRef(null);
 useEffect(()=>()=>request.current?.abort(),[]);
 const resolve=async()=>{
  if(lock.current||disabled||!code.trim())return;
  lock.current=true;setPending(true);setError("");
  const controller=new AbortController();request.current=controller;
  try{const product=await barcodes.lookup(code,{signal:controller.signal});
   if(controller.signal.aborted)return;
   await onResolved(product);if(!controller.signal.aborted)setCode("");
  }catch(issue){if(!controller.signal.aborted)setError(issue.message);}
  finally{lock.current=false;if(!controller.signal.aborted)setPending(false);}
 };
 return {code,setCode,pending,error,resolve};
}
