"use client";

import { useRef } from "react";
import { Canvas, useFrame } from "@react-three/fiber";
import * as THREE from "three";

function FloatingCard() {
  const meshRef = useRef<THREE.Mesh>(null);

  useFrame((state) => {
    if (meshRef.current) {
      meshRef.current.rotation.y =
        Math.sin(state.clock.elapsedTime * 0.4) * 0.15;
      meshRef.current.rotation.x =
        Math.sin(state.clock.elapsedTime * 0.3) * 0.08;
      meshRef.current.position.y =
        Math.sin(state.clock.elapsedTime * 0.5) * 0.08;
    }
  });

  return (
    <mesh ref={meshRef}>
      <boxGeometry args={[1.8, 1.1, 0.06]} />
      <meshStandardMaterial
        color="#1b4332"
        transparent
        opacity={0.35}
        roughness={0.6}
      />
    </mesh>
  );
}

export function TripHeroVisual() {
  return (
    <Canvas
      camera={{ position: [0, 0, 3], fov: 45 }}
      dpr={[1, 1.5]}
      gl={{ antialias: false, alpha: true }}
    >
      <ambientLight intensity={0.9} />
      <directionalLight position={[2, 3, 2]} intensity={0.4} />
      <FloatingCard />
    </Canvas>
  );
}
