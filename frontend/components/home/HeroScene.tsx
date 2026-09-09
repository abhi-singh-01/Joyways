"use client";

import { useEffect, useRef, useState } from "react";
import { Canvas, useFrame } from "@react-three/fiber";
import * as THREE from "three";

function Hills() {
  const far = useRef<THREE.Mesh>(null);
  const mid = useRef<THREE.Mesh>(null);

  useFrame((state) => {
    const t = state.clock.elapsedTime;
    if (far.current) {
      far.current.position.y = -0.85 + Math.sin(t * 0.18) * 0.04;
    }
    if (mid.current) {
      mid.current.position.y = -1.05 + Math.sin(t * 0.22 + 1.2) * 0.05;
    }
  });

  return (
    <>
      <mesh ref={far} position={[0.4, -0.85, -2.4]} rotation={[-0.42, 0.18, 0.04]}>
        <sphereGeometry args={[3.4, 24, 16]} />
        <meshStandardMaterial
          color="#1b4332"
          transparent
          opacity={0.28}
          roughness={0.92}
          metalness={0}
          depthWrite={false}
        />
      </mesh>
      <mesh ref={mid} position={[-1.6, -1.05, -0.6]} rotation={[-0.38, -0.22, -0.06]}>
        <sphereGeometry args={[2.6, 22, 14]} />
        <meshStandardMaterial
          color="#2d6a4f"
          transparent
          opacity={0.22}
          roughness={0.9}
          metalness={0}
          depthWrite={false}
        />
      </mesh>
    </>
  );
}

function createMoteField(count: number, seed: number) {
  let state = seed >>> 0;
  const next = () => {
    state = (Math.imul(1664525, state) + 1013904223) >>> 0;
    return state / 4294967296;
  };
  const positions = new Float32Array(count * 3);
  const speeds = new Float32Array(count);
  for (let i = 0; i < count; i++) {
    positions[i * 3] = (next() - 0.5) * 10;
    positions[i * 3 + 1] = next() * 4 - 0.4;
    positions[i * 3 + 2] = (next() - 0.5) * 5;
    speeds[i] = 0.08 + next() * 0.18;
  }
  return { positions, speeds };
}

const MOTE_FIELD = createMoteField(70, 20260909);

function Motes() {
  const points = useRef<THREE.Points>(null);
  const { positions, speeds } = MOTE_FIELD;

  useFrame((state, delta) => {
    const mesh = points.current;
    if (!mesh) return;
    const attr = mesh.geometry.getAttribute("position");
    for (let i = 0; i < speeds.length; i++) {
      let y = attr.getY(i) + speeds[i] * delta;
      if (y > 3.6) y = -0.6;
      attr.setY(i, y);
    }
    attr.needsUpdate = true;
    mesh.rotation.y = state.clock.elapsedTime * 0.02;
  });

  return (
    <points ref={points}>
      <bufferGeometry>
        <bufferAttribute attach="attributes-position" args={[positions, 3]} />
      </bufferGeometry>
      <pointsMaterial
        color="#fefae0"
        size={0.035}
        transparent
        opacity={0.55}
        sizeAttenuation
        depthWrite={false}
      />
    </points>
  );
}

export function HeroScene() {
  const wrapRef = useRef<HTMLDivElement>(null);
  const [active, setActive] = useState(true);

  useEffect(() => {
    const el = wrapRef.current;
    if (!el) return;
    const observer = new IntersectionObserver(
      ([entry]) => setActive(entry.isIntersecting),
      { threshold: 0.08 }
    );
    observer.observe(el);
    return () => observer.disconnect();
  }, []);

  return (
    <div ref={wrapRef} className="h-full w-full">
      <Canvas
        frameloop={active ? "always" : "never"}
        dpr={[1, 1.5]}
        camera={{ position: [0, 0.9, 5.4], fov: 40 }}
        gl={{ antialias: false, alpha: true, powerPreference: "low-power" }}
      >
        <ambientLight intensity={0.85} />
        <directionalLight position={[3, 4, 2]} intensity={0.35} color="#d8f3dc" />
        <fog attach="fog" args={["#1b3a4a", 6, 14]} />
        <Hills />
        <Motes />
      </Canvas>
    </div>
  );
}
