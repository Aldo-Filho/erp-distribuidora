import CustomerAddressModal from "./CustomerAddressModal";

export default function CustomerAddresses(props) {
  return <CustomerAddressModal {...props} embedded onClose={() => {}} />;
}
