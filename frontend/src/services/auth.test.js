import { resolveApiUrl } from "./auth";

describe("resolveApiUrl", () => {
  const originalLocation = window.location;

  afterEach(() => {
    window.history.replaceState({}, "", "/");
  });

  afterAll(() => {
    window.location = originalLocation;
  });

  it("uses the backend host for local frontend requests", () => {
    delete window.location;
    window.location = new URL("http://localhost:3004/authentication/sign-up");

    expect(resolveApiUrl("/api/auth/register")).toBe("http://localhost:5000/api/auth/register");
  });

  it("uses the current origin for non-localhost deployments", () => {
    delete window.location;
    window.location = new URL("https://example.com/app");

    expect(resolveApiUrl("/api/auth/register")).toBe("https://example.com/api/auth/register");
  });
});
