// Turns any failed request into a readable message.
// Backend error format (GlobalExceptionHandler):
//   { status, error, message, fieldErrors?: { field: "message" } }
const formatField = (field) =>
  field.replace(/([A-Z])/g, " $1").replace(/^./, (c) => c.toUpperCase());

export function getErrorMessage(error, fallback = "Unexpected error. Please try again.") {
  const data = error?.response?.data;

  // 400 with one message per invalid field (@Valid on the request body)
  if (data?.fieldErrors && Object.keys(data.fieldErrors).length > 0) {
    return Object.entries(data.fieldErrors)
      .map(([field, message]) => `${formatField(field)}: ${message}`)
      .join(" · ");
  }
  if (data?.message) return data.message;
  if (typeof data === "string" && data.trim()) return data;

  // No response at all: backend down, network error, CORS...
  if (error?.request && !error?.response) return "Cannot reach the server. Please check your connection.";

  return error?.message || fallback;
}
