"use client";

import { useRef, useMemo, Suspense, useSyncExternalStore } from "react";
import { Canvas, useFrame } from "@react-three/fiber";
import * as THREE from "three";

function subscribeReducedMotion(callback: () => void) {
  const mq = window.matchMedia("(prefers-reduced-motion: reduce)");
  mq.addEventListener("change", callback);
  return () => mq.removeEventListener("change", callback);
}

function getReducedMotionSnapshot() {
  return window.matchMedia("(prefers-reduced-motion: reduce)").matches;
}

function MountainLayer({
  color,
  y,
  scale,
  speed,
}: {
  color: string;
  y: number;
  scale: number;
  speed: number;
}) {
  const meshRef = useRef<THREE.Mesh>(null);

  const geometry = useMemo(() => {
    const geo = new THREE.PlaneGeometry(12, 4, 32, 16);
    const pos = geo.attributes.position;
    for (let i = 0; i < pos.count; i++) {
      const x = pos.getX(i);
      const wave = Math.sin(x * 1.5) * 0.3 + Math.sin(x * 3) * 0.15;
      pos.setZ(i, wave);
    }
    geo.computeVertexNormals();
    return geo;
  }, []);

  useFrame((state) => {
    if (meshRef.current) {
      meshRef.current.position.y =
        y + Math.sin(state.clock.elapsedTime * speed) * 0.05;
    }
  });

  return (
    <mesh
      ref={meshRef}
      geometry={geometry}
      position={[0, y, -scale]}
      scale={[scale, scale * 0.4, 1]}
    >
      <meshStandardMaterial
        color={color}
        transparent
        opacity={0.6}
        side={THREE.DoubleSide}
      />
    </mesh>
  );
}

function Scene() {
  return (
    <>
      <ambientLight intensity={0.8} />
      <directionalLight position={[5, 5, 5]} intensity={0.5} />
      <MountainLayer color="#1b4332" y={-0.5} scale={2} speed={0.3} />
      <MountainLayer color="#2d6a4f" y={-0.8} scale={2.5} speed={0.2} />
      <MountainLayer color="#40916c" y={-1.1} scale={3} speed={0.15} />
    </>
  );
}

function StaticFallback() {
  return (
    <div className="h-full w-full bg-gradient-to-b from-forest/10 via-forest/5 to-transparent" />
  );
}

export function HeroMountains() {
  const reducedMotion = useSyncExternalStore(
    subscribeReducedMotion,
    getReducedMotionSnapshot,
    () => false
  );

  if (reducedMotion) {
    return <StaticFallback />;
  }

  return (
    <Canvas
      camera={{ position: [0, 0, 5], fov: 50 }}
      className="h-full w-full"
      dpr={[1, 1.5]}
      gl={{ antialias: false, alpha: true }}
    >
      <Suspense fallback={null}>
        <Scene />
      </Suspense>
    </Canvas>
  );
}
