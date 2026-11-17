import { useCallback, useEffect, useMemo, useState } from "react";
import { dashboard } from "../../services/dashboard";
import { resolveFilter } from "./dashboardCatalog";
export default function useDashboard() {
  const [preference,setPreference]=useState(null),[filter,setFilter]=useState(null);
  const [data,setData]=useState(null),[error,setError]=useState(""),[loading,setLoading]=useState(true);
  const [tick,setTick]=useState(0),[options,setOptions]=useState({unit:"",rankingMetric:"QUANTITY"});
  const [boot,setBoot]=useState(0);
  useEffect(()=>{
    const controller=new AbortController();let active=true;
    setLoading(true);setError("");
    dashboard.preference({signal:controller.signal}).then(p=>{if(active){setPreference(p);setFilter(p.filter);}})
      .catch(e=>{if(active && e.name!=="AbortError"){setError(e.message);setLoading(false);}});
    return ()=>{active=false;controller.abort();};
  },[boot]);
  const selection=useMemo(()=>{if(!filter)return null;try{return resolveFilter(filter);}catch(e){return {error:e.message};}},[filter]);
  const request=useMemo(()=>selection&&!selection.error?{...selection,...options}:null,[selection,options]);
  useEffect(()=>{
    if(!request)return;
    const controller=new AbortController();let active=true;
    setLoading(true);setError("");
    dashboard.current(request,{signal:controller.signal}).then(result=>{if(active)setData(result);})
      .catch(e=>{if(active && e.name!=="AbortError")setError(e.message);}).finally(()=>{if(active)setLoading(false);});
    return ()=>{active=false;controller.abort();};
  },[request,tick]);
  const reload=useCallback(()=>{if(preference)setTick(t=>t+1);else setBoot(t=>t+1);},[preference]);
  return {preference,setPreference,filter,setFilter,data,error:selection?.error||error,loading:selection?.error?false:loading,request,options,setOptions,reload};
}
