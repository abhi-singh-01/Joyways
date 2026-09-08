type RazorpayHandlerResponse = {
  razorpay_order_id: string;
  razorpay_payment_id: string;
  razorpay_signature: string;
};

type RazorpayOptions = {
  key: string;
  amount: number;
  currency: string;
  name: string;
  description: string;
  order_id: string;
  prefill?: {
    name?: string;
    email?: string;
    contact?: string;
  };
  theme?: { color?: string };
  handler: (response: RazorpayHandlerResponse) => void;
  modal?: {
    ondismiss?: () => void;
  };
};

type RazorpayPaymentFailedResponse = {
  error: {
    code: string;
    description: string;
    reason?: string;
  };
};

type RazorpayInstance = {
  open: () => void;
  on: (event: "payment.failed", handler: (response: RazorpayPaymentFailedResponse) => void) => void;
};

declare global {
  interface Window {
    Razorpay?: new (options: RazorpayOptions) => RazorpayInstance;
  }
}

const SCRIPT_SRC = "https://checkout.razorpay.com/v1/checkout.js";

let scriptPromise: Promise<void> | null = null;

export function loadRazorpayScript(): Promise<void> {
  if (typeof window === "undefined") {
    return Promise.reject(new Error("Razorpay can only be loaded in the browser"));
  }
  if (window.Razorpay) {
    return Promise.resolve();
  }
  if (scriptPromise) {
    return scriptPromise;
  }

  scriptPromise = new Promise((resolve, reject) => {
    const existing = document.querySelector(`script[src="${SCRIPT_SRC}"]`);
    if (existing) {
      existing.addEventListener("load", () => resolve());
      existing.addEventListener("error", () => reject(new Error("Failed to load Razorpay")));
      return;
    }

    const script = document.createElement("script");
    script.src = SCRIPT_SRC;
    script.async = true;
    script.onload = () => resolve();
    script.onerror = () => reject(new Error("Failed to load Razorpay checkout"));
    document.body.appendChild(script);
  });

  return scriptPromise;
}

export type OpenCheckoutParams = {
  keyId: string;
  orderId: string;
  amountInr: number;
  currency: string;
  customerName: string;
  customerEmail: string;
  customerMobile: string;
  packageTitle: string;
  onSuccess: (response: RazorpayHandlerResponse) => void;
  onDismiss?: () => void;
  onPaymentFailed?: (message: string) => void;
};

export async function openRazorpayCheckout(params: OpenCheckoutParams): Promise<void> {
  await loadRazorpayScript();

  if (!window.Razorpay) {
    throw new Error("Razorpay checkout is unavailable");
  }

  const publicKeyId =
    params.keyId || process.env.NEXT_PUBLIC_RAZORPAY_KEY_ID || "";
  if (!publicKeyId) {
    throw new Error("Razorpay key is not configured.");
  }

  const amountPaise = Math.round(params.amountInr * 100);

  const checkout = new window.Razorpay({
    key: publicKeyId,
    amount: amountPaise,
    currency: params.currency,
    name: "JOYWAY",
    description: params.packageTitle,
    order_id: params.orderId,
    prefill: {
      name: params.customerName,
      email: params.customerEmail,
      contact: params.customerMobile,
    },
    theme: { color: "#1B4332" },
    handler: params.onSuccess,
    modal: {
      ondismiss: params.onDismiss,
    },
  });

  checkout.on("payment.failed", (response) => {
    const message =
      response.error?.description || "Payment failed. Please try again.";
    params.onPaymentFailed?.(message);
  });

  checkout.open();
}

export type { RazorpayHandlerResponse };
