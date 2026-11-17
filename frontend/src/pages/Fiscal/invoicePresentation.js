import { characteristics } from "../Products/productFormData";
export const notice="DEMONSTRATIVO — SEM VALIDADE FISCAL";
export const money=value=>Number(value??0).toLocaleString("pt-BR",{style:"currency",currency:"BRL"});
export const quantity=value=>value==null?"Não informado":Number(value).toLocaleString("pt-BR",{maximumFractionDigits:4});
export const date=value=>value?String(value).slice(0,10).split("-").reverse().join("/"):"Não informado";
export const issued=value=>new Date(value).toLocaleString("pt-BR",{timeZone:"America/Sao_Paulo"});
export const payments={CASH:"Dinheiro",PIX:"Pix",CREDIT_CARD:"Cartão de crédito",DEBIT_CARD:"Cartão de débito",BANK_TRANSFER:"Transferência",BOLETO:"Boleto demonstrativo"};
export const addressType={MAIN:"Principal",SHIPPING:"Entrega",BILLING:"Cobrança",EMISSOR:"Estabelecimento"};
export function addressLine(a) { return [a.street,a.number,a.complement,a.neighborhood,a.city,a.state,a.country,a.zipCode].filter(Boolean).join(" · ")||"Endereço incompleto na emissão"; }

export function attributesLine(attributes) { return Object.entries(attributes||{}).map(([key,value])=>`${characteristics[key]||key}: ${value}`).join(" · "); }
