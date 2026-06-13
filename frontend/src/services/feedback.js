import { apiFetch } from "../utils/api";

export const submitFeedback = async ({ satisfactionScore, goodPoint, improvePoint }) => {
  return await apiFetch('/feedback', {
    method: 'POST',
    body: JSON.stringify({
      satisfactionScore,
      goodPoint,
      improvePoint,
    }),
  });
};