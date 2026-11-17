package com.pi.erp.product.barcode;
import com.pi.erp.product.*;
import com.pi.erp.auth.CurrentUser;
import com.pi.erp.exception.ResourceNotFoundException;
import jakarta.persistence.*;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Service
public class BarcodeService {
 @Autowired private BarcodeRepository repository;
 @Autowired private ProductRepository products;
 @Autowired private EntityManager em;
 public static String normalize(String code) {
  if(code==null) throw new IllegalArgumentException("Informe o código de barras.");
  String value=code.strip().toUpperCase(Locale.ROOT);
  if(!value.matches("[A-Z0-9._/-]{1,128}")) throw new IllegalArgumentException("Código inválido. Use letras, números, ponto, hífen, barra ou sublinhado, sem espaços.");
  return value;
 }
 @Transactional(readOnly=true)
 public Product resolve(String code) {
  Barcode barcode=repository.findByCode(normalize(code)).orElseThrow(()->new ResourceNotFoundException("Código de barras não encontrado."));
  if(!barcode.isActive()) throw new IllegalArgumentException("Código de barras inativo.");
  Product product=products.findById(barcode.getProductId()).orElseThrow(()->new ResourceNotFoundException("SKU não encontrado."));
  if(!product.isActive()) throw new IllegalArgumentException("O SKU deste código está inativo.");
  return product;
 }
 @Transactional
 public Barcode register(RequestBarcodeDTO data) {
  CurrentUser.require("products.update"); lockProduct(data.productId());
  String code=normalize(data.code());
  if(data.type()==BarcodeType.EAN_GTIN) {
   if(!code.matches("(?:[0-9]{8}|[0-9]{12}|[0-9]{13}|[0-9]{14})")) throw new IllegalArgumentException("EAN/GTIN exige 8, 12, 13 ou 14 dígitos.");
   int sum=0,weight=3;
   for(int i=code.length()-2;i>=0;i--) { sum+=(code.charAt(i)-'0')*weight; weight=weight==3?1:3; }
   if((10-sum%10)%10!=code.charAt(code.length()-1)-'0') throw new IllegalArgumentException("Dígito verificador EAN/GTIN inválido.");
  }
  if(repository.findByCode(code).isPresent()) throw new IllegalArgumentException("Este código já está cadastrado; não pode ser transferido para outro SKU.");
  Barcode barcode=new Barcode(); barcode.setProductId(data.productId()); barcode.setCode(code); barcode.setType(data.type());
  if(Boolean.TRUE.equals(data.mainCode())) { clearMain(data.productId()); barcode.setMainCode(true); }
  return repository.save(barcode);
 }
 @Transactional
 public Barcode update(Long id,PatchBarcodeDTO data) {
  CurrentUser.require("products.update");
  Barcode found=repository.findById(id).orElseThrow(()->new ResourceNotFoundException("Código não encontrado."));
  lockProduct(found.getProductId()); em.refresh(found);
  boolean active=data.active()==null?found.isActive():data.active();
  boolean main=data.mainCode()==null?found.isMainCode():data.mainCode();
  if(!active) main=false;
  if(main) clearMain(found.getProductId());
  found.setActive(active); found.setMainCode(main); return found;
 }
 private void lockProduct(Long id) {
  Product product=products.findById(id).orElseThrow(()->new ResourceNotFoundException("SKU não encontrado."));
  em.lock(product,LockModeType.PESSIMISTIC_WRITE); em.refresh(product);
 }
 private void clearMain(Long productId) {
  repository.findByProductIdOrderById(productId).forEach(b->b.setMainCode(false)); repository.flush();
 }
}
